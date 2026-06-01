package com.example.leitordocumento_compose.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import kotlin.math.max
import kotlin.math.min

/**
 * AnalisadorTextoFrame v2
 *
 * Melhorias para cameras fracas e dispositivos antigos:
 *  1. Extrai canal Y do YUV diretamente — luminância sem custo de decode
 *  2. Equalização de histograma local (CLAHE simplificado) — melhora contraste
 *  3. Sharpening via kernel Laplaciano — texto borrado fica mais legível
 *  4. Bitmap reutilizável — sem pressão de GC em dispositivos com pouca RAM
 *  5. Flag de processamento — evita enfileiramento de frames durante análise
 */
class AnalisadorTextoFrame(
    private val reconhecedorTexto: TextRecognizer,
    private val onTexto: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var ultimaAnalise = 0L
    private val INTERVALO_MS = 500L

    // Flag para evitar processamento paralelo em devices lentos
    @Volatile
    private var processando = false

    // Bitmap reutilizável — alocado uma vez, evita GC
    private var bitmapReutilizavel: Bitmap? = null

    @ExperimentalGetImage
    override fun analyze(imagemProxy: ImageProxy) {
        val agora = System.currentTimeMillis()

        // Descarta frames se ainda estiver processando ou no intervalo
        if (agora - ultimaAnalise < INTERVALO_MS || processando) {
            imagemProxy.close()
            return
        }

        ultimaAnalise = agora
        processando = true

        try {
            val bitmap = extrairEProcessarBitmap(imagemProxy)

            if (bitmap == null) {
                // Fallback: usa InputImage direto se a extração manual falhar
                val mediaImage = imagemProxy.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        imagemProxy.imageInfo.rotationDegrees
                    )
                    reconhecedorTexto.process(inputImage)
                        .addOnSuccessListener { onTexto(it.text) }
                        .addOnCompleteListener {
                            processando = false
                            imagemProxy.close()
                        }
                } else {
                    processando = false
                    imagemProxy.close()
                }
                return
            }

            // Cria InputImage a partir do bitmap processado
            val inputImage = InputImage.fromBitmap(
                bitmap,
                imagemProxy.imageInfo.rotationDegrees
            )

            reconhecedorTexto.process(inputImage)
                .addOnSuccessListener { mlTexto ->
                    onTexto(mlTexto.text)
                }
                .addOnCompleteListener {
                    processando = false
                    imagemProxy.close()
                }

        } catch (e: Exception) {
            processando = false
            imagemProxy.close()
        }
    }

    /**
     * Extrai o canal Y (luminância) do YUV_420_888 e aplica:
     *  - Equalização de histograma (melhora contraste em câmeras ruins)
     *  - Sharpening Laplaciano (texto borrado fica mais definido)
     *
     * O canal Y do YUV já é grayscale puro — sem custo de conversão de cor!
     */
    private fun extrairEProcessarBitmap(proxy: ImageProxy): Bitmap? {
        return try {
            val planoY = proxy.planes[0]
            val largura = proxy.width
            val altura = proxy.height
            val rowStride = planoY.rowStride
            val pixelStride = planoY.pixelStride

            val bufferY = planoY.buffer

            // Extrai pixels de luminância brutos
            val pixelsGray = IntArray(largura * altura)
            for (y in 0 until altura) {
                for (x in 0 until largura) {
                    val bufferIndex = y * rowStride + x * pixelStride
                    val luminancia = bufferY.get(bufferIndex).toInt() and 0xFF
                    pixelsGray[y * largura + x] = luminancia
                }
            }

            // Equalização de histograma — melhora contraste global
            val pixelsEqualizados = equalizarHistograma(pixelsGray, largura, altura)

            // Sharpening — reforça bordas do texto
            val pixelsSharpenados = aplicarSharpening(pixelsEqualizados, largura, altura)

            // Monta Bitmap ARGB_8888 reutilizável
            val bitmap = obterOuCriarBitmap(largura, altura)
            val argbPixels = IntArray(largura * altura) { i ->
                val v = pixelsSharpenados[i]
                (0xFF shl 24) or (v shl 16) or (v shl 8) or v  // R=G=B=v (grayscale)
            }
            bitmap.setPixels(argbPixels, 0, largura, 0, 0, largura, altura)
            bitmap

        } catch (e: Exception) {
            null  // Fallback para o caminho padrão do ML Kit
        }
    }

    /**
     * Equalização de histograma simples (global).
     * Redistribui os valores de luminância para usar toda a faixa 0-255.
     * Essencial para câmeras com exposição ruim ou documentos com reflexo.
     */
    private fun equalizarHistograma(pixels: IntArray, largura: Int, altura: Int): IntArray {
        val histograma = IntArray(256)
        for (v in pixels) histograma[v]++

        // CDF (função de distribuição acumulada)
        val cdf = IntArray(256)
        cdf[0] = histograma[0]
        for (i in 1..255) cdf[i] = cdf[i - 1] + histograma[i]

        val cdfMin = cdf.first { it > 0 }
        val total = largura * altura

        // Tabela de lookup — O(1) por pixel depois disso
        val lut = IntArray(256) { v ->
            val equalizado = ((cdf[v] - cdfMin).toFloat() / (total - cdfMin) * 255f + 0.5f).toInt()
            equalizado.coerceIn(0, 255)
        }

        return IntArray(pixels.size) { lut[pixels[it]] }
    }

    /**
     * Sharpening via Laplaciano unsharp mask simplificado.
     * Kernel 3x3:
     *   [ 0, -1,  0]
     *   [-1,  5, -1]
     *   [ 0, -1,  0]
     *
     * Reforça bordas — texto borrado se torna mais legível para o ML Kit.
     * Força controlada por `alpha` (0.0 = sem sharpening, 1.0 = máximo).
     */
    private fun aplicarSharpening(pixels: IntArray, largura: Int, altura: Int): IntArray {
        val saida = IntArray(pixels.size)
        val alpha = 0.6f  // Intensidade calibrada — agressivo demais gera ruído

        for (y in 0 until altura) {
            for (x in 0 until largura) {
                // Borda: copia sem filtrar
                if (x == 0 || x == largura - 1 || y == 0 || y == altura - 1) {
                    saida[y * largura + x] = pixels[y * largura + x]
                    continue
                }

                val centro = pixels[y * largura + x]
                val cima = pixels[(y - 1) * largura + x]
                val baixo = pixels[(y + 1) * largura + x]
                val esq = pixels[y * largura + (x - 1)]
                val dir = pixels[y * largura + (x + 1)]

                // Laplaciano
                val lap = 4 * centro - cima - baixo - esq - dir

                // Unsharp mask: original + alpha * laplaciano
                val aguçado = (centro + alpha * lap).toInt().coerceIn(0, 255)
                saida[y * largura + x] = aguçado
            }
        }
        return saida
    }

    /**
     * Reutiliza o mesmo Bitmap se as dimensões baterem.
     * Evita alocação de ~3MB por frame em dispositivos com pouca RAM.
     */
    private fun obterOuCriarBitmap(largura: Int, altura: Int): Bitmap {
        val existente = bitmapReutilizavel
        return if (existente != null &&
            existente.width == largura &&
            existente.height == altura &&
            !existente.isRecycled
        ) {
            existente
        } else {
            existente?.recycle()
            Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888).also {
                bitmapReutilizavel = it
            }
        }
    }

    /**
     * Libera recursos ao destruir o analisador.
     * Chamar em onDestroy/onPause do ciclo de vida.
     */
    fun liberar() {
        bitmapReutilizavel?.recycle()
        bitmapReutilizavel = null
    }
}