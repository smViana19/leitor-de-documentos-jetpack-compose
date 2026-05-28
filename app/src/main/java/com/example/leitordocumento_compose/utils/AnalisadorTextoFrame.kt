package com.example.leitordocumento_compose.utils

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer

class AnalisadorTextoFrame(
    private val reconhecedorTexto: TextRecognizer,
    private val onTexto: (String) -> Unit

): ImageAnalysis.Analyzer
{

    private var ultimaAnalise = 0L
    private val INTERVALO_MS = 500L
    @ExperimentalGetImage
    override fun analyze(imagemProxy: ImageProxy)
    {
        val agora = System.currentTimeMillis()
        if (agora - ultimaAnalise < INTERVALO_MS)
        {
            imagemProxy.close()
            return
        }
        ultimaAnalise = agora

        val mediaImage = imagemProxy.image ?: run { imagemProxy.close(); return }
        val inputImage = InputImage.fromMediaImage(mediaImage,
            imagemProxy.imageInfo.rotationDegrees)

        reconhecedorTexto.process(inputImage)
            .addOnSuccessListener { mlTexto ->
                onTexto(mlTexto.text)
            }
            .addOnCompleteListener { imagemProxy.close() }
    }


}