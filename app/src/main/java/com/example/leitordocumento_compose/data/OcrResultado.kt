package com.example.leitordocumento_compose.data

import ResultadoPlaca

sealed class OcrResultado {
    data class Cnh(val dadosCNH: DadosCNH) : OcrResultado()
    data class Rg(val dadosRG: DadosRG) : OcrResultado()
    data class Placa(val dadosPlaca: ResultadoPlaca) : OcrResultado()
    data class Desconhecido(val rawText: String) : OcrResultado()
}