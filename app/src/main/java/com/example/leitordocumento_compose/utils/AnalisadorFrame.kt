package com.example.leitordocumento_compose.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.graphics.Color
import com.example.documentscan.DocumentType
import com.example.leitordocumento_compose.presentation.ui.states.EstadoDocumento
import com.example.leitordocumento_compose.presentation.ui.states.FeedbackDocumento
import com.example.leitordocumento_compose.presentation.ui.states.ProporcoesDocumento
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class AnalisadorFrame(
    private val tipoDocumento: () -> DocumentType,
    private val onFeedback: (FeedbackDocumento) -> Unit
) : ImageAnalysis.Analyzer {

    private var ultimaAnalise = 0L
    private val intervaloMs = 200L

    override fun analyze(imagem: ImageProxy) {
        val tempoAtual = System.currentTimeMillis()
        if (tempoAtual - ultimaAnalise < intervaloMs) {
            imagem.close()
            return
        }
        ultimaAnalise = tempoAtual
        try {
            val bitmap = imagem.toBitmap()
            val feedback = analisarBitmap(
                bitmap = bitmap,
                larguraFrame = imagem.width,
                alturaFrame = imagem.height,
                tipo = tipoDocumento()
            )
            onFeedback(feedback)
        } finally {
            imagem.close()
        }

    }

    private fun analisarBitmap(
        bitmap: Bitmap,
        larguraFrame: Int,
        alturaFrame: Int,
        tipo: DocumentType
    ): FeedbackDocumento  {
        val luminosidade = calcularLuminosidade(bitmap)
        if (luminosidade < 40f) {
            return FeedbackDocumento(
                estado    = EstadoDocumento.RUIM_LUZ,
                mensagem  = "Ambiente muito escuro — aproxime de uma luz",
                corOverlay = Color(0xFFE24B4A),
                progresso  = luminosidade / 40f
            )
        }
        if (luminosidade > 220f) {
            return FeedbackDocumento(
                estado    = EstadoDocumento.RUIM_LUZ,
                mensagem  = "Luz em excesso — evite reflexos",
                corOverlay = Color(0xFFBA7517),
                progresso  = 0.3f
            )
        }
        val nitidez = calcularNitidez(bitmap)
        if (nitidez < 80f) {
            return FeedbackDocumento(
                estado     = EstadoDocumento.DESFOCADO,
                mensagem   = "Imagem borrada — mantenha firme",
                corOverlay  = Color(0xFFBA7517),
                progresso   = nitidez / 80f
            )
        }
        val propAlvo = when (tipo) {
            DocumentType.ID_CARD -> ProporcoesDocumento.CNH
            DocumentType.DOCUMENT -> ProporcoesDocumento.RG
            DocumentType.PASSPORT -> 0.7f
            DocumentType.BOOK -> 0.75f
        }

        val areaRelativa = calcularAreaDocumentoEstimada(bitmap)

        return when {
            areaRelativa < 0.25f -> FeedbackDocumento(
                estado     = EstadoDocumento.DETECTANDO,
                mensagem   = "Aproxime o documento do quadro",
                corOverlay  = Color(0xFF8A9BB5),
                progresso   = areaRelativa / 0.25f
            )
            areaRelativa < 0.45f -> FeedbackDocumento(
                estado     = EstadoDocumento.ALINHANDO,
                mensagem   = "Centralize e aproxime um pouco mais",
                corOverlay  = Color(0xFF4A90D9),
                progresso   = (areaRelativa - 0.25f) / 0.20f
            )
            areaRelativa > 0.90f -> FeedbackDocumento(
                estado     = EstadoDocumento.ALINHANDO,
                mensagem   = "Afaste um pouco o documento",
                corOverlay  = Color(0xFFBA7517),
                progresso   = 0.7f
            )
            else -> FeedbackDocumento(
                estado     = EstadoDocumento.PERFEITO,
                mensagem   = "Documento detectado",
                corOverlay  = Color(0xFF4A90D9),
                progresso   = 1f
            )
        }
    }

    private fun calcularLuminosidade(bitmap: Bitmap): Float {
        val scaled = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
        var soma = 0L
        val pixels = IntArray(64*64)
        scaled.getPixels(pixels, 0, 64, 0, 0, 64, 64)
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8)  and 0xFF
            val b =  pixel         and 0xFF
            soma += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
        }
        scaled.recycle()
        return soma.toFloat() / (64 * 64)
    }

    private fun calcularNitidez(bitmap: Bitmap): Float {
        val scaled = Bitmap.createScaledBitmap(bitmap, 128, 128, false)
        val pixels = IntArray(128 * 128)
        scaled.getPixels(pixels, 0, 128, 0, 0, 128, 128)
        scaled.recycle()

        val grays = pixels.map { p ->
            val r = (p shr 16) and 0xFF
            val g = (p shr 8)  and 0xFF
            val b =  p         and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b)
        }
        val media = grays.average()
        val variancia = grays.map { v -> (v - media) * (v - media) }.average()
        return variancia.toFloat()
    }

    /**
     * Estima a área do documento pela diferença de brilho entre centro e bordas.
     * Substitua por detecção de contorno real quando integrar OpenCV/MLKit Selfie Seg.
     */

    private fun calcularAreaDocumentoEstimada(bitmap: Bitmap): Float {
        val scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, false)
        val pixels = IntArray(32 * 32)
        scaled.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        scaled.recycle()

        fun grayAt(x: Int, y: Int): Float {
            val p = pixels[y * 32 + x]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8)  and 0xFF
            val b =  p         and 0xFF
            return (0.299f * r + 0.587f * g + 0.114f * b)
        }

        var somaCentro = 0f
        for (y in 12..19) for (x in 12..19) somaCentro += grayAt(x, y)
        val mediaCentro = somaCentro / 64f

        var somaBorda = 0f
        for (x in 0..31) { somaBorda += grayAt(x, 0); somaBorda += grayAt(x, 31) }
        for (y in 1..30) { somaBorda += grayAt(0, y); somaBorda += grayAt(31, y) }
        val mediaBorda = somaBorda / (32 * 4 - 4).toFloat()
        val contraste = abs(mediaCentro - mediaBorda) / 255f
        return (contraste * 3f).coerceIn(0f, 1f)

    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }


}