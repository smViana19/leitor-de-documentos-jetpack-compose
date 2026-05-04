package com.example.leitordocumento_compose.utils

import com.example.documentscan.DocumentType
import com.google.mlkit.vision.text.Text


data class DadosCNH(
    val nome: String? = null,
    val cpf: String? = null,
    val rg: String? = null,
    val dataNascimento: String? = null,
    val numeroRegistro: String? = null,
    val dataEmissao: String? = null,
    val dataValidade: String? = null,
    val categoria: String? = null,
    val localidade: String? = null,
    val rawText: String = ""
)

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

sealed class OcrResultado {
    data class Cnh(val dadosCNH: DadosCNH) : OcrResultado()
    data class Rg(val dadosRG: DadosRG) : OcrResultado()
    data class Unknown(val rawText: String) : OcrResultado()
}

object DocumentoOcrProcessador {
    private val regexData = Regex("""(\d{2}[/\-.]\d{2}[/\-.]\d{4})""")
    private val regexCpf = Regex("""(\d{3}[.\- ]?\d{3}[.\- ]?\d{3}[.\- ]?\d{2})""")
    private val regexRg = Regex("""(\d{1,2}[.\- ]?\d{3}[.\- ]?\d{3}[.\- ]?[\dXx])""")

    fun processarDocumento(mlKitTexto: Text, tipoDocumentoSelecionado: DocumentType): OcrResultado {
        val textoCompleto = mlKitTexto.text
        val linhas = textoCompleto
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return when {
            isCnh(textoCompleto, linhas) -> OcrResultado.Cnh(processarCnh(linhas, textoCompleto))
            isRg(textoCompleto, linhas) -> OcrResultado.Rg(processarRg(linhas, textoCompleto))
            else -> OcrResultado.Unknown(rawText = textoCompleto)
        }
    }

    private fun isCnh(texto: String, linhas: List<String>): Boolean {
        val textoMaiusculo = texto.uppercase()
        return textoMaiusculo.contains("HABILITAÇÃO") ||
                textoMaiusculo.contains("CARTEIRA NACIONAL") ||
                textoMaiusculo.contains("CNH") ||
                textoMaiusculo.contains("DETRAN") ||
                linhas.any { it.matches(Regex("[ABCDE]{1,5}")) }
    }

    private fun isRg(texto: String, linhas: List<String>): Boolean {
        val textoMaiusculo = texto.uppercase()
        return textoMaiusculo.contains("IDENTIDADE") ||
                textoMaiusculo.contains("REGISTRO GERAL") ||
                textoMaiusculo.contains("REPÚBLICA FEDERATIVA") ||
                textoMaiusculo.contains("SECRETARIA") && textoMaiusculo.contains("SEGURANÇA")
    }

    private fun possuiPalavrasChaveCnh(texto: String) {
        texto.uppercase().let {
            it.contains("VALIDADE") || it.contains("CATEGORIA") || it.contains("REGISTRO")
        }
    }


    private fun processarCnh(linhas: List<String>, rawText: String): DadosCNH {

        val datas = regexData.findAll(rawText).map { it.value }.toList()
        val cpf = regexCpf.find(rawText)?.value?.normalize()
        val reg = extractNumberAfterLabel(linhas, listOf("REGISTRO", "N°", "Nº"))

        val nome = linhas.firstOrNull() { linha ->
            linha.uppercase() == linha &&
                    linha.split(" ").size >= 2 &&
                    linha.length > 6 &&
                    !linha.any() { it.isDigit() } &&
                    !linha.contains("HABILITAÇÃO") &&
                    !linha.contains("DETRAN") &&
                    !linha.contains("BRASIL")
        }

        val categoria =
            linhas.firstOrNull { it.trim().matches(Regex("[ABCDEabcde]{1,5}")) }?.uppercase()
        val validade = if (datas.size >= 2) datas.last() else datas.firstOrNull()
        val emissao = if (datas.size >= 2) datas[datas.size - 2] else null
        val nascimento = datas.firstOrNull()
        val localidade =
            extractLineAfterKeyword(linhas, listOf("NATURALIDADE", "LOCAL", "MUNICÍPIO"))

        return DadosCNH(
            nome = nome,
            cpf = cpf,
            rg = extractRg(linhas, rawText),
            dataNascimento = nascimento,
            numeroRegistro = reg,
            dataEmissao = emissao,
            dataValidade = validade,
            categoria = categoria,
            localidade = localidade,
            rawText = rawText
        )

    }

    private fun processarRg(linhas: List<String>, rawText: String): DadosRG {
        val datas = regexData.findAll(rawText).map { it.value }.toList()
        val cpf = regexCpf.find(rawText)?.value?.normalize()
        val numeroRg = extractRg(linhas, rawText)

        val nome = linhas.firstOrNull { linha ->
            linha.uppercase() == linha &&
                    linha.split(" ").size >= 2 &&
                    linha.length > 6 &&
                    !linha.any { it.isDigit() } &&
                    !linha.contains("REPÚBLICA") &&
                    !linha.contains("BRASIL") &&
                    !linha.contains("IDENTIDADE")
        }

        val nomeMae = extractLineAfterKeyword(linhas, listOf("MÃE", "MAE", "FILIAÇÃO"))
        val nomePai = extractLineAfterKeyword(linhas, listOf("PAI"))
        val naturalidade = extractLineAfterKeyword(linhas, listOf("NATURALIDADE", "NATURAL"))

        return DadosRG(
            nome = nome,
            rg = numeroRg,
            cpf = cpf,
            dataNascimento = datas.firstOrNull(),
            nomeMae = nomeMae,
            nomePai = nomePai,
            naturalidade = naturalidade,
            dataEmissao = datas.lastOrNull(),
            rawText = rawText
        )
    }

    /** Extrai o número de RG preferindo o formato XX.XXX.XXX-X */
    private fun extractRg(lines: List<String>, raw: String): String? {
        // Tenta pegar o que vem após palavra-chave "RG" ou "IDENTIDADE"
        val afterLabel =
            extractNumberAfterLabel(lines, listOf("RG", "IDENTIDADE", "REGISTRO GERAL"))
        if (afterLabel != null) return afterLabel

        return regexRg.find(raw)?.value
    }

    /** Pega o conteúdo da linha imediatamente após uma das palavras-chave */
    private fun extractLineAfterKeyword(lines: List<String>, keywords: List<String>): String? {
        val idx = lines.indexOfFirst { line ->
            keywords.any { kw -> line.uppercase().contains(kw) }
        }
        return if (idx >= 0 && idx + 1 < lines.size) lines[idx + 1] else null
    }

    /** Extrai número que aparece na mesma linha que um dos labels */
    private fun extractNumberAfterLabel(lines: List<String>, labels: List<String>): String? {
        val line = lines.firstOrNull { l -> labels.any { lb -> l.uppercase().contains(lb) } }
            ?: return null
        return Regex("""[\d.\-/Xx]+""").find(line.substringAfter(":").trim())?.value
    }

    private fun String.normalize() = replace(Regex("""[.\- ]"""), "")
        .let { raw ->
            // formata CPF: 000.000.000-00
            if (raw.length == 11)
                "${raw.substring(0, 3)}.${raw.substring(3, 6)}.${
                    raw.substring(
                        6,
                        9
                    )
                }-${raw.substring(9)}"
            else raw
        }


}

