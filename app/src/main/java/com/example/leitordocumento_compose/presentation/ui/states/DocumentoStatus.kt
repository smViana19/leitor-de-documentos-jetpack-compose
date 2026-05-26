package com.example.leitordocumento_compose.presentation.ui.states

import androidx.compose.ui.graphics.Color


enum class EstadoDocumento {
    NENHUM,       // nada detectado
    DETECTANDO,   // algo encontrado, analisando
    ALINHANDO,    // documento detectado, ajustando posição
    PERFEITO,     // pronto para capturar
    RUIM_LUZ,     // pouca luz
    DESFOCADO     // imagem borrada
}

data class FeedbackDocumento(
    val estado: EstadoDocumento = EstadoDocumento.NENHUM,
    val mensagem: String = "Posicione o documento no quadro",
    val corOverlay: Color = Color(0xFF8A9BB5),
    val progresso: Float = 0f
)

object ProporcoesDocumento {
    const val CNH     = 85.6f / 54f
    const val RG      = 1.4f
    const val A4      = 210f / 297f
    const val MARGEM  = 0.18f
}