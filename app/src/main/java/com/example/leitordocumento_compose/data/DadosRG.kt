package com.example.leitordocumento_compose.data

data class DadosRG(
    val nome: String? = null,
    val rg: String? = null,
    val cpf: String? = null,
    val dataNascimento: String? = null,
    val nomeMae: String? = null,
    val nomePai: String? = null,
    val naturalidade: String? = null,
    val dataEmissao: String? = null,
    val rawText: String = ""
)