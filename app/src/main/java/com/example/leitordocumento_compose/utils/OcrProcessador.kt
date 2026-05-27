package com.example.leitordocumento_compose.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.documentscan.DocumentType
import com.example.leitordocumento_compose.presentation.ui.states.FeedbackDocumento
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.emptyList


/**
 * OcrProcessador v2 — captura múltipla com seleção do melhor resultado.
 *
 * Melhorias principais:
 *  1. Captura N frames em sequência rápida e escolhe o que retornou mais campos preenchidos
 *  2. Merge inteligente: combina campos de múltiplas capturas (cobre leituras parciais)
 *  3. Retry automático em caso de resultado insatisfatório
 *  4. Fila de processamento para não bloquear a câmera
 */

class OcrProcessador(
    private val contexto: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val tipoDocumentoSelecionado: () -> DocumentType,
    private val onResultado: (OcrResultado) -> Unit,
    private val onError: (String) -> Unit
) {
    private val reconhecedorTexto = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imagemCaptura: ImageCapture? = null

    private var analisadorFrame: AnalisadorFrame? = null

    private var camera: Camera? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var cameraProvider: ProcessCameraProvider? = null

    private val TOTAL_CAPTURAS = 3
    private val DELAY_ENTRE_CAPTURAS_MS = 300L
    private var capturasEmAndamento = false

    fun bindCamera(
        previewView: PreviewView,
        onFeedback: (FeedbackDocumento) -> Unit = {}
    ) {
        val future = ProcessCameraProvider.getInstance(contexto)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            imagemCaptura = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val analisador = AnalisadorFrame(
                tipoDocumento = tipoDocumentoSelecionado,
                onFeedback = onFeedback
            )

            val analiseImagem = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, analisador) }
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imagemCaptura,
                    analiseImagem
                )
            } catch (e: Exception) {
                onError("Erro ao iniciar câmera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(contexto))


    }

    fun ligarFlash(estadoAtual: Boolean): Boolean {
        val novoEstado = !estadoAtual
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() ?: false

        if (hasFlash) {
            camera?.cameraControl?.enableTorch(novoEstado)
        }
        return if (hasFlash) novoEstado else false
    }

    fun capturarEProcessar() {

        if (capturasEmAndamento) return
        capturasEmAndamento = true
        val captura = imagemCaptura ?: run {
            onError("Câmera não inicializada")
            capturasEmAndamento = false
            return
        }
        capturarMultiplos(captura, TOTAL_CAPTURAS, emptyList())


    }

    private fun capturarMultiplos(
        captura: ImageCapture,
        restantes: Int,
        bitmapsAcumulados: List<Bitmap>
    ) {
        if (restantes == 0) {
            processarMultiplosBitmaps(bitmapsAcumulados)
            return
        }
        captura.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(proxy: ImageProxy) {
                try {
                    val bitmap =
                        proxy.toBitmap().rotateTo(proxy.imageInfo.rotationDegrees.toFloat())
                    proxy.close()
                    val novos = bitmapsAcumulados + bitmap

                    if (restantes > 1) {
                        Thread.sleep(DELAY_ENTRE_CAPTURAS_MS)
                        capturarMultiplos(captura, restantes - 1, novos)
                    } else {
                        capturarMultiplos(captura, 0, novos)
                    }
                } catch (e: Exception) {
                    proxy.close()
                    if (bitmapsAcumulados.isNotEmpty()) {
                        processarMultiplosBitmaps(bitmapsAcumulados)
                    } else {
                        capturasEmAndamento = false
                        onError("Erro ao capturar: ${e.message}")
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                if (bitmapsAcumulados.isNotEmpty()) {
                    processarMultiplosBitmaps(bitmapsAcumulados)
                } else {
                    capturasEmAndamento = false
                    onError("Erro ao capturar imagem: ${exception.message}")
                }
            }
        })
    }

    private fun processarMultiplosBitmaps(bitmaps: List<Bitmap>) {

        val resultados = mutableListOf<OcrResultado>()
        var processados = 0

        bitmaps.forEachIndexed { index, bitmap ->
            val image = InputImage.fromBitmap(bitmap, 0)
            reconhecedorTexto.process(image)
                .addOnSuccessListener { mlText ->
                    Log.d("OCR_RAW", "Frame $index:\n${mlText.text}")
                    val resultado = DocumentoOcrProcessador.processarDocumento(
                        mlText, tipoDocumentoSelecionado()
                    )
                    synchronized(resultados) {
                        resultados.add(resultado)
                        processados++
                        if (processados == bitmaps.size) {
                            capturasEmAndamento = false
                            val melhor = selecionarMelhorResultado(resultados)
                            onResultado(melhor)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    synchronized(resultados) {
                        processados++
                        if (processados == bitmaps.size) {
                            capturasEmAndamento = false
                            if (resultados.isNotEmpty()) {
                                onResultado(selecionarMelhorResultado(resultados))
                            } else {
                                onError("OCR falhou em todos os frames: ${e.message}")
                            }
                        }
                    }
                }
        }
    }

    private fun selecionarMelhorResultado(resultados: List<OcrResultado>): OcrResultado {
        if (resultados.isEmpty()) return OcrResultado.Unknown("")
        if (resultados.size == 1) return resultados[0]

        val cnhs = resultados.filterIsInstance<OcrResultado.Cnh>()
        val rgs = resultados.filterIsInstance<OcrResultado.Rg>()

        return when {
            cnhs.size >= resultados.size / 2 -> mergeCnh(cnhs)
            rgs.size >= resultados.size / 2 -> mergeRg(rgs)
            else -> resultados.maxByOrNull { pontuarResultado(it) } ?: resultados[0]
        }
    }

    private fun mergeCnh(cnhs: List<OcrResultado.Cnh>): OcrResultado.Cnh {
        // Ordena por pontuação descendente — o melhor é a base
        val ordenados = cnhs.sortedByDescending { pontuarCnh(it.dadosCNH) }
        var base = ordenados[0].dadosCNH

        for (i in 1 until ordenados.size) {
            val outro = ordenados[i].dadosCNH
            base = base.copy(
                nome = base.nome ?: outro.nome,
                cpf = base.cpf ?: outro.cpf,
                rg = base.rg ?: outro.rg,
                dataNascimento = base.dataNascimento ?: outro.dataNascimento,
                localNascimento = base.localNascimento ?: outro.localNascimento,
                numeroRegistro = base.numeroRegistro ?: outro.numeroRegistro,
                primeiraHabilitacao = base.primeiraHabilitacao ?: outro.primeiraHabilitacao,
                dataEmissao = base.dataEmissao ?: outro.dataEmissao,
                dataValidade = base.dataValidade ?: outro.dataValidade,
                categoria = base.categoria ?: outro.categoria,
                orgaoEmissor = base.orgaoEmissor ?: outro.orgaoEmissor,
                filiacao = base.filiacao ?: outro.filiacao,
                nacionalidade = base.nacionalidade ?: outro.nacionalidade,
                // rawText acumula todos os textos para debug
                rawText = (base.rawText + "\n---\n" + outro.rawText).trim()
            )
        }
        return OcrResultado.Cnh(base)
    }

    private fun mergeRg(rgs: List<OcrResultado.Rg>): OcrResultado.Rg {
        val ordenados = rgs.sortedByDescending { pontuarRg(it.dadosRG) }
        var base = ordenados[0].dadosRG

        for (i in 1 until ordenados.size) {
            val outro = ordenados[i].dadosRG
            base = base.copy(
                nome = base.nome ?: outro.nome,
                rg = base.rg ?: outro.rg,
                cpf = base.cpf ?: outro.cpf,
                dataNascimento = base.dataNascimento ?: outro.dataNascimento,
                nomeMae = base.nomeMae ?: outro.nomeMae,
                nomePai = base.nomePai ?: outro.nomePai,
                naturalidade = base.naturalidade ?: outro.naturalidade,
                dataEmissao = base.dataEmissao ?: outro.dataEmissao,
                rawText = (base.rawText + "\n---\n" + outro.rawText).trim()
            )
        }
        return OcrResultado.Rg(base)
    }

    private fun pontuarResultado(r: OcrResultado) = when (r) {
        is OcrResultado.Cnh -> pontuarCnh(r.dadosCNH)
        is OcrResultado.Rg -> pontuarRg(r.dadosRG)
        is OcrResultado.Unknown -> 0
    }

    private fun pontuarCnh(d: DadosCNH): Int {
        var score = 0
        if (!d.nome.isNullOrBlank()) score += 3
        if (!d.cpf.isNullOrBlank()) score += 3
        if (!d.rg.isNullOrBlank()) score += 2
        if (!d.dataNascimento.isNullOrBlank()) score += 2
        if (!d.dataValidade.isNullOrBlank()) score += 2
        if (!d.dataEmissao.isNullOrBlank()) score += 1
        if (!d.numeroRegistro.isNullOrBlank()) score += 2
        if (!d.categoria.isNullOrBlank()) score += 2
        if (!d.localNascimento.isNullOrBlank()) score += 1
        if (!d.orgaoEmissor.isNullOrBlank()) score += 1
        if (!d.filiacao.isNullOrBlank()) score += 1
        return score
    }

    private fun pontuarRg(d: DadosRG): Int {
        var score = 0
        if (!d.nome.isNullOrBlank()) score += 3
        if (!d.rg.isNullOrBlank()) score += 3
        if (!d.cpf.isNullOrBlank()) score += 3
        if (!d.dataNascimento.isNullOrBlank()) score += 2
        if (!d.nomeMae.isNullOrBlank()) score += 2
        if (!d.nomePai.isNullOrBlank()) score += 1
        if (!d.naturalidade.isNullOrBlank()) score += 1
        return score
    }

    fun processarBitmapExterno(bitmap: Bitmap) = processarMultiplosBitmaps(listOf(bitmap))

    fun shutdown() {
        executor.shutdown()
        reconhecedorTexto.close()
    }


    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun Bitmap.rotateTo(degrees: Float): Bitmap {
        if (degrees == 0f) return this
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }


}