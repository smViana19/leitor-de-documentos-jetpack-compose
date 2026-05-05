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
import com.example.leitordocumento_compose.ui.states.FeedbackDocumento
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
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

    fun setZoom(valor: Float) {
        camera?.cameraControl?.setLinearZoom(valor)
    }

    fun trocarCamera(previewView: PreviewView) {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraSelector.DEFAULT_FRONT_CAMERA
        else
            CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider?.let { provider ->
            bindCamera(previewView)
        }
    }


    fun capturarEProcessar() {
        val captura = imagemCaptura ?: run {
            onError("Câmera não inicializada")
            return
        }

        captura.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(proxy: ImageProxy) {
                try {
                    val bitmap =
                        proxy.toBitmap().rotateTo(proxy.imageInfo.rotationDegrees.toFloat())
                    proxy.close()
                    runOcr(bitmap)
                } catch (e: Exception) {
                    proxy.close()
                    onError("Erro ao processar imagem: ${e.message}")
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError("Erro ao capturar imagem: ${exception.message}")
            }
        })

    }

    fun processarBitmapExterno(bitmap: Bitmap) = runOcr(bitmap)
    private fun runOcr(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        reconhecedorTexto.process(image)
            .addOnSuccessListener { mlText ->
                Log.d("OCR_RAW", mlText.text)
                val result =
                    DocumentoOcrProcessador.processarDocumento(mlText, tipoDocumentoSelecionado())
                onResultado(result)
            }
            .addOnFailureListener { e ->
                onError("OCR falhou: ${e.message}")
            }
    }

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