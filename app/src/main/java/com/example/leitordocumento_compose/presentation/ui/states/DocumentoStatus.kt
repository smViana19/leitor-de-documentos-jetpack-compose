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
    val mensagemVoz: String = "Vire o telefone" ,
    val corOverlay: Color = Color(0xFF8A9BB5),
    val progresso: Float = 0f
)

object ProporcoesDocumento {
    const val CNH = 1.585f     // cartão padrão ISO 7810
    const val RG = 1.414f      // A-ratio aproximado
    const val CRLV = 0.707f    // portrait
    const val PLACA_CARRO = 3.076f
    const val PLACA_MOTO = 0.56f
}