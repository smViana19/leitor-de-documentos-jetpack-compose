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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * AnalisadorFrame v2 — estabilizado e com detecção mais robusta.
 *
 * Melhorias principais:
 *  1. Histórico de N frames para suavizar oscilações (média móvel)
 *  2. Cálculo de nitidez via variância do Laplaciano (mais preciso que variância simples)
 *  3. Detecção de área via gradiente de borda (melhor que contraste centro/borda)
 *  4. Histeria de estado: só muda de PERFEITO → outro estado após K frames consecutivos ruins
 *  5. Luminosidade com percentis para ignorar reflexos pontuais
 */
class AnalisadorFrame(
    private val tipoDocumento: () -> DocumentType,
    private val onFeedback: (FeedbackDocumento) -> Unit
) : ImageAnalysis.Analyzer {

    // ── Throttling ────────────────────────────────────────────────────────────
    private var ultimaAnalise = 0L
    private val intervaloMs = 150L          // 6-7 FPS de análise — suficiente e menos CPU

    // ── Histórico para suavização ─────────────────────────────────────────────
    private val JANELA_HISTORICO = 5
    private val historicoLuminosidade = ArrayDeque<Float>(JANELA_HISTORICO)
    private val historicoNitidez = ArrayDeque<Float>(JANELA_HISTORICO)
    private val historicoArea = ArrayDeque<Float>(JANELA_HISTORICO)

    // ── Histeria de estado ────────────────────────────────────────────────────
    private var estadoAtual = EstadoDocumento.NENHUM
    private var contadorEstadoNovo = 0
    private val LIMIAR_MUDANCA_ESTADO = 3   // precisa de 3 frames consecutivos para mudar estado

    // ── Thresholds calibrados ─────────────────────────────────────────────────
    private val LUZ_MIN = 45f
    private val LUZ_MAX = 215f
    private val NITIDEZ_MIN = 120f          // variância do Laplaciano — valor empírico
    private val AREA_MUITO_PEQUENA = 0.22f
    private val AREA_BOA_MIN = 0.40f
    private val AREA_BOA_MAX = 0.88f

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
                tipo = tipoDocumento()
            )
            onFeedback(feedback)
        } finally {
            imagem.close()
        }
    }

    private fun analisarBitmap(bitmap: Bitmap, tipo: DocumentType): FeedbackDocumento {
        // Reduz para análise — 96x96 é bom equilíbrio entre precisão e velocidade
        val scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, true)
        val pixels = IntArray(96 * 96)
        scaled.getPixels(pixels, 0, 96, 0, 0, 96, 96)
        scaled.recycle()

        val grays = FloatArray(96 * 96) { i ->
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            0.299f * r + 0.587f * g + 0.114f * b
        }

        // ── Métricas brutas ───────────────────────────────────────────────────
        val lumBruta = calcularLuminosidadePercentil(grays)
        val nitBruta = calcularVarianciaLaplaciano(grays, 96, 96)
        val areaBruta = calcularAreaDocumentoPorGradiente(grays, 96, 96)

        // ── Suavização por média móvel ────────────────────────────────────────
        val luminosidade = adicionarEMedia(historicoLuminosidade, lumBruta)
        val nitidez = adicionarEMedia(historicoNitidez, nitBruta)
        val areaRelativa = adicionarEMedia(historicoArea, areaBruta)

        // ── Determina feedback candidato ──────────────────────────────────────
        val candidato = calcularFeedback(luminosidade, nitidez, areaRelativa)

        // ── Aplica histeria de estado ─────────────────────────────────────────
        return aplicarHisteria(candidato)
    }

    // ── Luminosidade por percentil (ignora reflexos/sombras pontuais) ─────────
    private fun calcularLuminosidadePercentil(grays: FloatArray): Float {
        val sorted = grays.copyOf().also { it.sort() }
        // Usa mediana (P50) — muito mais estável que média
        return sorted[sorted.size / 2]
    }

    // ── Variância do Laplaciano (padrão-ouro para nitidez) ───────────────────
    // Calcula o laplaciano de cada pixel interior e retorna a variância.
    // Alto → imagem nítida; baixo → borrada.
    private fun calcularVarianciaLaplaciano(grays: FloatArray, w: Int, h: Int): Float {
        var soma = 0.0
        var somaSq = 0.0
        var count = 0

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val lap = (-4f * grays[y * w + x]
                        + grays[(y - 1) * w + x]
                        + grays[(y + 1) * w + x]
                        + grays[y * w + (x - 1)]
                        + grays[y * w + (x + 1)])
                soma += lap
                somaSq += lap * lap
                count++
            }
        }
        val media = soma / count
        return ((somaSq / count) - (media * media)).toFloat().coerceAtLeast(0f)
    }

    // ── Área por magnitude de gradiente (Sobel simplificado) ─────────────────
    // Detecta bordas fortes que indicam a presença de um documento retangular.
    // Muito mais estável que a heurística centro/borda anterior.
    private fun calcularAreaDocumentoPorGradiente(grays: FloatArray, w: Int, h: Int): Float {
        // Analisa só a faixa central (descarta bordas da câmera)
        val xMin = w / 6;
        val xMax = w * 5 / 6
        val yMin = h / 6;
        val yMax = h * 5 / 6
        val totalPixels = (xMax - xMin) * (yMax - yMin)

        var pixelsFortes = 0
        val limiarGradiente = 25f          // borda forte mínima

        for (y in yMin until yMax) {
            for (x in xMin until xMax) {
                if (x == 0 || x == w - 1 || y == 0 || y == h - 1) continue
                val gx = grays[y * w + (x + 1)] - grays[y * w + (x - 1)]
                val gy = grays[(y + 1) * w + x] - grays[(y - 1) * w + x]
                val mag = sqrt(gx * gx + gy * gy)
                if (mag > limiarGradiente) pixelsFortes++
            }
        }

        // Normaliza: quanto mais bordas, mais provável que haja documento
        // Mapeia [0 → 0.15 de pixels fortes] para [0 → 1]
        return (pixelsFortes.toFloat() / totalPixels / 0.15f).coerceIn(0f, 1f)
    }

    // ── Média móvel ───────────────────────────────────────────────────────────
    private fun adicionarEMedia(fila: ArrayDeque<Float>, valor: Float): Float {
        if (fila.size >= JANELA_HISTORICO) fila.removeFirst()
        fila.addLast(valor)
        return fila.average().toFloat()
    }

    // ── Lógica principal de feedback ──────────────────────────────────────────
    private fun calcularFeedback(lum: Float, nit: Float, area: Float): FeedbackDocumento {
        // 1. Luminosidade primeiro (problema ambiental, mais urgente)
        if (lum < LUZ_MIN) return FeedbackDocumento(
            estado = EstadoDocumento.RUIM_LUZ,
            mensagem = "Ambiente escuro — aproxime de uma fonte de luz",
            corOverlay = Color(0xFFE24B4A),
            progresso = (lum / LUZ_MIN).coerceIn(0f, 1f)
        )
        if (lum > LUZ_MAX) return FeedbackDocumento(
            estado = EstadoDocumento.RUIM_LUZ,
            mensagem = "Reflexo excessivo — incline levemente o documento",
            corOverlay = Color(0xFFBA7517),
            progresso = 0.3f
        )

        // 2. Nitidez (câmera ainda focando)
        if (nit < NITIDEZ_MIN) return FeedbackDocumento(
            estado = EstadoDocumento.DESFOCADO,
            mensagem = "Mantenha o celular firme…",
            corOverlay = Color(0xFFBA7517),
            progresso = (nit / NITIDEZ_MIN).coerceIn(0f, 1f)
        )

        // 3. Área / proximidade
        return when {
            area < AREA_MUITO_PEQUENA -> FeedbackDocumento(
                estado = EstadoDocumento.DETECTANDO,
                mensagem = "Aproxime o documento do quadro",
                corOverlay = Color(0xFF8A9BB5),
                progresso = (area / AREA_MUITO_PEQUENA).coerceIn(0f, 1f)
            )

            area < AREA_BOA_MIN -> FeedbackDocumento(
                estado = EstadoDocumento.ALINHANDO,
                mensagem = "Centralize e aproxime um pouco mais",
                corOverlay = Color(0xFF4A90D9),
                progresso = ((area - AREA_MUITO_PEQUENA) / (AREA_BOA_MIN - AREA_MUITO_PEQUENA)).coerceIn(
                    0f,
                    1f
                )
            )

            area > AREA_BOA_MAX -> FeedbackDocumento(
                estado = EstadoDocumento.ALINHANDO,
                mensagem = "Afaste um pouco o documento",
                corOverlay = Color(0xFFBA7517),
                progresso = 0.6f
            )

            else -> FeedbackDocumento(
                estado = EstadoDocumento.PERFEITO,
                mensagem = "✓ Documento posicionado",
                corOverlay = Color(0xFF1D9E75),
                progresso = 1f
            )
        }
    }

    // ── Histeria: evita mudanças de estado rápidas demais ────────────────────
    private fun aplicarHisteria(candidato: FeedbackDocumento): FeedbackDocumento {
        if (candidato.estado == estadoAtual) {
            contadorEstadoNovo = 0
            return candidato
        }
        contadorEstadoNovo++
        // Mudança imediata para PERFEITO requer mais confirmações (evita falsos positivos)
        val limiar = if (candidato.estado == EstadoDocumento.PERFEITO) LIMIAR_MUDANCA_ESTADO + 1
        else LIMIAR_MUDANCA_ESTADO
        return if (contadorEstadoNovo >= limiar) {
            estadoAtual = candidato.estado
            contadorEstadoNovo = 0
            candidato
        } else {
            // Mantém o estado anterior mas atualiza progresso/mensagem levemente
            candidato.copy(estado = estadoAtual)
        }
    }

    // ── Conversão ImageProxy → Bitmap ─────────────────────────────────────────
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
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 92, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}