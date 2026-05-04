package com.example.leitordocumento_compose.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.MatrixExt.postRotate
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.documentscan.DocumentType
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

    fun bindCamera(previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(contexto)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imagemCaptura = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imagemCaptura
                )
            } catch (e: Exception) {
                onError("Erro ao iniciar câmera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(contexto))


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