package com.example.leitordocumento_compose.data

data class DadosCNH(
    val nome: String? = null,
    val cpf: String? = null,
    val rg: String? = null,
    val dataNascimento: String? = null,
    val localNascimento: String? = null,
    val numeroRegistro: String? = null,
    val primeiraHabilitacao: String? = null,
    val dataEmissao: String? = null,
    val dataValidade: String? = null,
    val categoria: String? = null,
    val orgaoEmissor: String? = null,
    val filiacao: String? = null,
    val nacionalidade: String? = null,
    val localidade: String? = null,
    val rawText: String = ""
)