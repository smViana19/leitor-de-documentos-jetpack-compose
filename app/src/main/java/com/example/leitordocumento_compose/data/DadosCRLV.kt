package com.example.leitordocumento_compose.data


data class DadosCRLV(
    val placa: String? = null,
    val renavam: String? = null,
    val chassi: String? = null,
    val proprietario: String? = null,
    val cpfCnpjProprietario: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val anoFabricacao: String? = null,
    val anoModelo: String? = null,
    val corPredominante: String? = null,
    val combustivel: String? = null,
    val especie: String? = null,
    val tipo: String? = null,
    val categoria: String? = null,
    val municipio: String? = null,
    val uf: String? = null,
    val validade: String? = null,
    val exercicio: String? = null,
    val cmt: String? = null,
    val pbt: String? = null,
    val capacidade: String? = null, val potencia: String? = null,
    val cilindrada: String? = null,
    val rawText: String = ""
)