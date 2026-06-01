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
import com.example.leitordocumento_compose.data.DadosCNH
import com.example.leitordocumento_compose.data.DadosCRLV
import com.example.leitordocumento_compose.data.DadosRG
import com.example.leitordocumento_compose.data.OcrResultado
import com.example.leitordocumento_compose.presentation.ui.states.EstadoDocumento
import com.example.leitordocumento_compose.presentation.ui.states.FeedbackDocumento
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.emptyList


/**
 * OcrProcessador v3 — merge inteligente com votação por campo.
 *
 * Principais melhorias sobre a v2:
 *  1. mergeCnh/mergeRg/mergeCrlv agora usam votação por campo para campos de texto
 *     (nome, proprietário, etc.) — pega o valor mais frequente entre frames, não apenas o primeiro
 *  2. Campos numéricos críticos (chassi, renavam, cpf) também recebem votação
 *  3. SCORE_EARLY_EXIT aumentado — só sai cedo se o resultado realmente for bom
 *  4. Log de campos extraídos para diagnóstico
 */
class OcrProcessador(
    private val contexto: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val tipoDocumentoSelecionado: () -> DocumentType,
    private val onResultado: (OcrResultado) -> Unit,
    private val onProgresso: ((Float) -> Unit)? = null,
    private val onError: (String) -> Unit,
    private val processadorCustom: ((String) -> OcrResultado)? = null,
    private val onFrameTexto: ((String) -> Boolean)? = null
) {

    // FIX: score mínimo para saída antecipada — mais alto = mais qualidade exigida
    // CNH/RG têm score máximo ~20; CRLV ~18. Early exit em 80% do máximo.
    private val SCORE_EARLY_EXIT = 16

    private val reconhecedorTexto = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imagemCaptura: ImageCapture? = null

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val TOTAL_CAPTURAS = 5           // FIX: aumentado de 3 → 5 para mais material de merge
    private val DELAY_ENTRE_CAPTURAS_MS = 250L
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

            val analiseImagem = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { imageAnalysis ->
                    if (onFrameTexto != null) {
                        imageAnalysis.setAnalyzer(executor, AnalisadorTextoFrame(
                            reconhecedorTexto = reconhecedorTexto,
                            onTexto = { texto ->
                                val deveCapturar = onFrameTexto.invoke(texto)
                                if (deveCapturar && !capturasEmAndamento) {
                                    capturarEProcessar()
                                }
                                val temPlaca = texto.length in 5..10
                                onFeedback(
                                    FeedbackDocumento(
                                        estado = if (temPlaca) EstadoDocumento.PERFEITO else EstadoDocumento.DETECTANDO,
                                        mensagem = if (temPlaca) "Placa detectada!" else "Procurando placa...",
                                        progresso = if (temPlaca) 1f else 0f
                                    )
                                )
                            }
                        ))
                    } else {
                        imageAnalysis.setAnalyzer(executor, AnalisadorFrame(
                            tipoDocumento = tipoDocumentoSelecionado,
                            onFeedback = onFeedback
                        ))
                    }
                }

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
        if (hasFlash) camera?.cameraControl?.enableTorch(novoEstado)
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

        val total = TOTAL_CAPTURAS
        val feitos = total - restantes
        onProgresso?.invoke(feitos.toFloat() / total)

        captura.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(proxy: ImageProxy) {
                try {
                    val bitmap = proxy.toBitmap().rotateTo(proxy.imageInfo.rotationDegrees.toFloat())
                    proxy.close()
                    val novos = bitmapsAcumulados + bitmap

                    if (novos.size >= 2 && restantes > 1) {
                        processarFrameRapido(novos.last()) { score ->
                            if (score >= SCORE_EARLY_EXIT) {
                                Log.d("OCR_SCORE", "Early exit com score=$score em ${novos.size} frames")
                                processarMultiplosBitmaps(novos)
                            } else {
                                Thread.sleep(DELAY_ENTRE_CAPTURAS_MS)
                                capturarMultiplos(captura, restantes - 1, novos)
                            }
                        }
                    } else {
                        capturarMultiplos(captura, restantes - 1, novos)
                    }
                } catch (e: Exception) {
                    proxy.close()
                    if (bitmapsAcumulados.isNotEmpty()) processarMultiplosBitmaps(bitmapsAcumulados)
                    else {
                        capturasEmAndamento = false
                        onError("Erro ao capturar: ${e.message}")
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                if (bitmapsAcumulados.isNotEmpty()) processarMultiplosBitmaps(bitmapsAcumulados)
                else {
                    capturasEmAndamento = false
                    onError("Erro ao capturar imagem: ${exception.message}")
                }
            }
        })
    }

    private fun processarFrameRapido(bitmap: Bitmap, onScore: (Int) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        reconhecedorTexto.process(image)
            .addOnSuccessListener { mlText ->
                val resultado = processadorCustom?.invoke(mlText.text)
                    ?: DocumentoOcrProcessador.processarDocumento(mlText, tipoDocumentoSelecionado())
                onScore(pontuarResultado(resultado))
            }
            .addOnFailureListener { onScore(0) }
    }

    private fun processarMultiplosBitmaps(bitmaps: List<Bitmap>) {
        val resultados = mutableListOf<OcrResultado>()
        var processados = 0

        bitmaps.forEachIndexed { index, bitmap ->
            val image = InputImage.fromBitmap(bitmap, 0)
            reconhecedorTexto.process(image)
                .addOnSuccessListener { mlText ->
                    Log.d("OCR_RAW", "Frame $index (score aprox ${
                        pontuarResultado(
                            processadorCustom?.invoke(mlText.text)
                                ?: DocumentoOcrProcessador.processarDocumento(mlText, tipoDocumentoSelecionado())
                        )
                    }):\n${mlText.text.take(300)}")

                    val resultado = processadorCustom?.invoke(mlText.text)
                        ?: DocumentoOcrProcessador.processarDocumento(mlText, tipoDocumentoSelecionado())

                    synchronized(resultados) {
                        resultados.add(resultado)
                        processados++
                        if (processados == bitmaps.size) {
                            capturasEmAndamento = false
                            val melhor = selecionarMelhorResultado(resultados)
                            logResultadoFinal(melhor)
                            onResultado(melhor)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    synchronized(resultados) {
                        processados++
                        if (processados == bitmaps.size) {
                            capturasEmAndamento = false
                            if (resultados.isNotEmpty()) onResultado(selecionarMelhorResultado(resultados))
                            else onError("OCR falhou em todos os frames: ${e.message}")
                        }
                    }
                }
        }
    }

    private fun logResultadoFinal(resultado: OcrResultado) {
        when (resultado) {
            is OcrResultado.Cnh -> Log.d("OCR_FINAL", "CNH: nome=${resultado.dadosCNH.nome}, " +
                    "cpf=${resultado.dadosCNH.cpf}, chassi=N/A, " +
                    "categoria=${resultado.dadosCNH.categoria}, score=${pontuarCnh(resultado.dadosCNH)}")
            is OcrResultado.Crlv -> Log.d("OCR_FINAL", "CRLV: placa=${resultado.dadosCRLV.placa}, " +
                    "renavam=${resultado.dadosCRLV.renavam}, chassi=${resultado.dadosCRLV.chassi}, " +
                    "proprietario=${resultado.dadosCRLV.proprietario}, score=${pontuarCrlv(resultado.dadosCRLV)}")
            is OcrResultado.Rg -> Log.d("OCR_FINAL", "RG: nome=${resultado.dadosRG.nome}, " +
                    "rg=${resultado.dadosRG.rg}, score=${pontuarRg(resultado.dadosRG)}")
            else -> {}
        }
    }

    private fun selecionarMelhorResultado(resultados: List<OcrResultado>): OcrResultado {
        if (resultados.isEmpty()) return OcrResultado.Desconhecido("")
        if (resultados.size == 1) return resultados[0]

        val cnhs = resultados.filterIsInstance<OcrResultado.Cnh>()
        val rgs = resultados.filterIsInstance<OcrResultado.Rg>()
        val crlvs = resultados.filterIsInstance<OcrResultado.Crlv>()
        val placas = resultados.filterIsInstance<OcrResultado.Placa>()

        return when {
            cnhs.size >= resultados.size / 2 -> mergeCnh(cnhs)
            rgs.size >= resultados.size / 2 -> mergeRg(rgs)
            crlvs.size >= resultados.size / 2 -> mergeCrlv(crlvs)
            placas.size >= resultados.size / 2 -> mergePlaca(placas)
            else -> resultados.maxByOrNull { pontuarResultado(it) } ?: resultados[0]
        }
    }

    private fun mergePlaca(placas: List<OcrResultado.Placa>): OcrResultado.Placa {
        return placas
            .groupBy { it.dadosPlaca }
            .maxByOrNull { it.value.size }
            ?.value?.first()
            ?: placas[0]
    }

    private fun mergeCnh(cnhs: List<OcrResultado.Cnh>): OcrResultado.Cnh {
        val ordenados = cnhs.sortedByDescending { pontuarCnh(it.dadosCNH) }
        var base = ordenados[0].dadosCNH

        for (i in 1 until ordenados.size) {
            val outro = ordenados[i].dadosCNH
            base = base.copy(
                // FIX: campos de texto usam votação — pega o mais frequente entre todos os frames
                nome = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.nome }) ?: base.nome ?: outro.nome,
                cpf = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.cpf }) ?: base.cpf ?: outro.cpf,
                rg = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.rg }) ?: base.rg ?: outro.rg,
                dataNascimento = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.dataNascimento })
                    ?: base.dataNascimento ?: outro.dataNascimento,
                localNascimento = base.localNascimento ?: outro.localNascimento,
                numeroRegistro = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.numeroRegistro })
                    ?: base.numeroRegistro ?: outro.numeroRegistro,
                primeiraHabilitacao = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.primeiraHabilitacao })
                    ?: base.primeiraHabilitacao ?: outro.primeiraHabilitacao,
                dataEmissao = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.dataEmissao })
                    ?: base.dataEmissao ?: outro.dataEmissao,
                dataValidade = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.dataValidade })
                    ?: base.dataValidade ?: outro.dataValidade,
                categoria = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.categoria })
                    ?: base.categoria ?: outro.categoria,
                orgaoEmissor = base.orgaoEmissor ?: outro.orgaoEmissor,
                filiacao = base.filiacao ?: outro.filiacao,
                nacionalidade = base.nacionalidade ?: outro.nacionalidade,
                rawText = (base.rawText + "\n---\n" + outro.rawText).trim()
            )
        }
        // Aplica votação final nos campos críticos usando todos os frames
        return OcrResultado.Cnh(base).let { OcrResultado.Cnh(base.copy(
            nome = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.nome }) ?: base.nome,
            cpf = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.cpf }) ?: base.cpf,
            categoria = votarCampoTexto(cnhs.mapNotNull { it.dadosCNH.categoria }) ?: base.categoria
        )) }
    }

    private fun mergeRg(rgs: List<OcrResultado.Rg>): OcrResultado.Rg {
        val ordenados = rgs.sortedByDescending { pontuarRg(it.dadosRG) }
        var base = ordenados[0].dadosRG

        for (i in 1 until ordenados.size) {
            val outro = ordenados[i].dadosRG
            base = base.copy(
                nome = votarCampoTexto(rgs.mapNotNull { it.dadosRG.nome }) ?: base.nome ?: outro.nome,
                rg = votarCampoTexto(rgs.mapNotNull { it.dadosRG.rg }) ?: base.rg ?: outro.rg,
                cpf = votarCampoTexto(rgs.mapNotNull { it.dadosRG.cpf }) ?: base.cpf ?: outro.cpf,
                dataNascimento = votarCampoTexto(rgs.mapNotNull { it.dadosRG.dataNascimento })
                    ?: base.dataNascimento ?: outro.dataNascimento,
                nomeMae = base.nomeMae ?: outro.nomeMae,
                nomePai = base.nomePai ?: outro.nomePai,
                naturalidade = base.naturalidade ?: outro.naturalidade,
                dataEmissao = votarCampoTexto(rgs.mapNotNull { it.dadosRG.dataEmissao })
                    ?: base.dataEmissao ?: outro.dataEmissao,
                rawText = (base.rawText + "\n---\n" + outro.rawText).trim()
            )
        }
        return OcrResultado.Rg(base)
    }

    private fun mergeCrlv(crlvs: List<OcrResultado.Crlv>): OcrResultado.Crlv {
        val ordenados = crlvs.sortedByDescending { pontuarCrlv(it.dadosCRLV) }
        var base = ordenados[0].dadosCRLV

        for (i in 1 until ordenados.size) {
            val outro = ordenados[i].dadosCRLV
            base = base.copy(
                // FIX CRÍTICO: chassi e renavam usam votação — o mais frequente é o correto
                placa = votarCampoTexto(crlvs.mapNotNull { it.dadosCRLV.placa }) ?: base.placa ?: outro.placa,
                renavam = votarCampoTexto(crlvs.mapNotNull { it.dadosCRLV.renavam })
                    ?: base.renavam ?: outro.renavam,
                chassi = votarCampoTexto(crlvs.mapNotNull { it.dadosCRLV.chassi })
                    ?: selecionarChassiMaisLongo(crlvs.mapNotNull { it.dadosCRLV.chassi })
                    ?: base.chassi ?: outro.chassi,
                proprietario = votarCampoTexto(crlvs.mapNotNull { it.dadosCRLV.proprietario })
                    ?: base.proprietario ?: outro.proprietario,
                cpfCnpjProprietario = votarCampoTexto(crlvs.mapNotNull { it.dadosCRLV.cpfCnpjProprietario })
                    ?: base.cpfCnpjProprietario ?: outro.cpfCnpjProprietario,
                marca = base.marca ?: outro.marca,
                modelo = base.modelo ?: outro.modelo,
                anoFabricacao = votarCampoTexto(crlvs.mapNotNull { it.dadosCRLV.anoFabricacao })
                    ?: base.anoFabricacao ?: outro.anoFabricacao,
                anoModelo = votarCampoTexto(crlvs.mapNotNull { it.dadosCRLV.anoModelo })
                    ?: base.anoModelo ?: outro.anoModelo,
                corPredominante = base.corPredominante ?: outro.corPredominante,
                combustivel = base.combustivel ?: outro.combustivel,
                especie = base.especie ?: outro.especie,
                tipo = base.tipo ?: outro.tipo,
                categoria = base.categoria ?: outro.categoria,
                municipio = base.municipio ?: outro.municipio,
                uf = base.uf ?: outro.uf,
                validade = votarCampoTexto(crlvs.mapNotNull { it.dadosCRLV.validade })
                    ?: base.validade ?: outro.validade,
                exercicio = votarCampoTexto(crlvs.mapNotNull { it.dadosCRLV.exercicio })
                    ?: base.exercicio ?: outro.exercicio,
                potencia = base.potencia ?: outro.potencia,
                cilindrada = base.cilindrada ?: outro.cilindrada,
                pbt = base.pbt ?: outro.pbt,
                cmt = base.cmt ?: outro.cmt,
                capacidade = base.capacidade ?: outro.capacidade,
                rawText = (base.rawText + "\n---\n" + outro.rawText).trim()
            )
        }
        return OcrResultado.Crlv(base)
    }

    /**
     * FIX: Votação por campo — retorna o valor mais frequente entre todos os frames.
     * Normaliza antes de comparar (ignora variações de formatação/espaço).
     * Se empate, retorna o valor mais longo (tende a ser mais completo).
     */
    private fun votarCampoTexto(valores: List<String>): String? {
        if (valores.isEmpty()) return null
        if (valores.size == 1) return valores[0]

        return valores
            .groupBy { it.trim().uppercase().replace(Regex("""\s+"""), " ") }
            .maxByOrNull { (_, grupo) -> grupo.size * 1000 + grupo.maxOf { it.length } }
            ?.value
            ?.maxByOrNull { it.length }  // entre os do grupo vencedor, pega o mais completo
    }

    /**
     * Para chassi: se não há consenso por votação, usa o candidato de 17 chars.
     * Múltiplas leituras parciais (16, 17 chars) — pega o de 17.
     */
    private fun selecionarChassiMaisLongo(chassiList: List<String>): String? =
        chassiList.filter { it.length == 17 }.firstOrNull()
            ?: chassiList.maxByOrNull { it.length }

    // ── Pontuação ─────────────────────────────────────────────────────────────

    private fun pontuarResultado(r: OcrResultado): Int = when (r) {
        is OcrResultado.Cnh -> pontuarCnh(r.dadosCNH)
        is OcrResultado.Rg -> pontuarRg(r.dadosRG)
        is OcrResultado.Placa -> 1
        is OcrResultado.Desconhecido -> 0
        is OcrResultado.Crlv -> pontuarCrlv(r.dadosCRLV)
    }

    private fun pontuarCrlv(dados: DadosCRLV): Int {
        var score = 0
        if (!dados.placa.isNullOrBlank()) score += 3
        if (!dados.renavam.isNullOrBlank()) score += 3
        if (!dados.chassi.isNullOrBlank()) score += 3  // FIX: chassi vale tanto quanto renavam
        if (!dados.proprietario.isNullOrBlank()) score += 2
        if (!dados.cpfCnpjProprietario.isNullOrBlank()) score += 2
        if (!dados.marca.isNullOrBlank()) score += 1
        if (!dados.modelo.isNullOrBlank()) score += 1
        if (!dados.exercicio.isNullOrBlank()) score += 1
        if (!dados.anoFabricacao.isNullOrBlank()) score += 1
        if (!dados.municipio.isNullOrBlank()) score += 1
        return score
    }

    private fun pontuarCnh(dadosCNH: DadosCNH): Int {
        var score = 0
        if (!dadosCNH.nome.isNullOrBlank()) score += 3
        if (!dadosCNH.cpf.isNullOrBlank()) score += 3
        if (!dadosCNH.rg.isNullOrBlank()) score += 2
        if (!dadosCNH.dataNascimento.isNullOrBlank()) score += 2
        if (!dadosCNH.dataValidade.isNullOrBlank()) score += 2
        if (!dadosCNH.dataEmissao.isNullOrBlank()) score += 1
        if (!dadosCNH.numeroRegistro.isNullOrBlank()) score += 2
        if (!dadosCNH.categoria.isNullOrBlank()) score += 2
        if (!dadosCNH.localNascimento.isNullOrBlank()) score += 1
        if (!dadosCNH.orgaoEmissor.isNullOrBlank()) score += 1
        if (!dadosCNH.filiacao.isNullOrBlank()) score += 1
        return score
    }

    private fun pontuarRg(dadosRG: DadosRG): Int {
        var score = 0
        if (!dadosRG.nome.isNullOrBlank()) score += 3
        if (!dadosRG.rg.isNullOrBlank()) score += 3
        if (!dadosRG.cpf.isNullOrBlank()) score += 3
        if (!dadosRG.dataNascimento.isNullOrBlank()) score += 2
        if (!dadosRG.nomeMae.isNullOrBlank()) score += 2
        if (!dadosRG.nomePai.isNullOrBlank()) score += 1
        if (!dadosRG.naturalidade.isNullOrBlank()) score += 1
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