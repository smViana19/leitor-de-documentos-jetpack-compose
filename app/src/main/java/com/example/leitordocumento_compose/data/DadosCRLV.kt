package com.example.leitordocumento_compose.data

/**
 * Modelo de dados extraídos por OCR do CRLV (Certificado de Registro
 * e Licenciamento de Veículo).
 *
 * Campos baseados no layout padrão DENATRAN/SENATRAN (modelos 2010–2024).
 */
data class DadosCRLV(
    /** Placa no formato Mercosul (ABC1D23) ou antigo (ABC-1234) */
    val placa: String? = null,

    /** RENAVAM — 9 ou 11 dígitos */
    val renavam: String? = null,

    /** Número de chassi — 17 caracteres alfanuméricos (VIN) */
    val chassi: String? = null,

    /** Nome do proprietário ou empresa */
    val proprietario: String? = null,

    /** CPF ou CNPJ do proprietário */
    val cpfCnpjProprietario: String? = null,

    /** Marca / fabricante (ex: VOLKSWAGEN, FIAT) */
    val marca: String? = null,

    /** Modelo / versão (ex: GOL 1.0 CITY) */
    val modelo: String? = null,

    /** Ano de fabricação (AAAA) */
    val anoFabricacao: String? = null,

    /** Ano do modelo (AAAA) */
    val anoModelo: String? = null,

    /** Cor predominante */
    val corPredominante: String? = null,

    /** Combustível (GASOLINA, ÁLCOOL, FLEX, DIESEL…) */
    val combustivel: String? = null,

    /** Espécie (PASSAGEIRO, CARGA, MISTO…) */
    val especie: String? = null,

    /** Tipo (AUTOMOVEL, CAMINHAO, MOTOCICLETA…) */
    val tipo: String? = null,

    /** Categoria (PARTICULAR, ALUGUEL, OFICIAL…) */
    val categoria: String? = null,

    /** Município de emplacamento */
    val municipio: String? = null,

    /** UF de emplacamento */
    val uf: String? = null,

    /** Validade do licenciamento (DD/MM/AAAA ou MM/AAAA) */
    val validade: String? = null,

    /** Exercício / ano do licenciamento */
    val exercicio: String? = null,

    /** Capacidade máxima de tração (CMT) em kg */
    val cmt: String? = null,

    /** Peso bruto total (PBT) em kg */
    val pbt: String? = null,

    /** Capacidade máxima de passageiros */
    val capacidade: String? = null,

    /** Potência do motor (cv) */
    val potencia: String? = null,

    /** Cilindrada */
    val cilindrada: String? = null,

    /** Texto bruto extraído pelo ML Kit (para debug) */
    val rawText: String = ""
)