package com.example.leitordocumento_compose.utils

import com.example.documentscan.DocumentType
import com.google.mlkit.vision.text.Text

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

    // ── Regexes gerais ────────────────────────────────────────────────────────

    /** Data no formato DD/MM/AAAA (aceita / . -) */
    private val regexData = Regex("""(\d{2}[/.\-]\d{2}[/.\-]\d{4})""")

    /** CPF: 000.000.000-00 ou variações sem pontuação */
    private val regexCpf = Regex("""(\d{3}[.\- ]?\d{3}[.\- ]?\d{3}[.\- ]?\d{2})""")

    /** Número de registro CNH: sequência de 9 a 11 dígitos */
    private val regexRegistroCnh = Regex("""\b(\d{9,11})\b""")

    /** RG: padrões comuns XX.XXX.XXX-X ou só dígitos com 7-9 chars */
    private val regexRg = Regex("""(\d{1,2}\.?\d{3}\.?\d{3}-?[\dXx])""")

    /** Categoria CNH: A, B, AB, C, D, E e combinações */
    private val regexCategoria = Regex("""^[ABCDEabcde]{1,5}$""")

    // ── Ponto de entrada ──────────────────────────────────────────────────────

    fun processarDocumento(mlKitTexto: Text, tipoDocumentoSelecionado: DocumentType): OcrResultado {
        val textoCompleto = mlKitTexto.text
        val linhas = textoCompleto
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return when {
            isCnh(textoCompleto) -> OcrResultado.Cnh(processarCnh(linhas, textoCompleto))
            isRg(textoCompleto) -> OcrResultado.Rg(processarRg(linhas, textoCompleto))
            else -> OcrResultado.Unknown(rawText = textoCompleto)
        }
    }

    // ── Detecção de tipo ──────────────────────────────────────────────────────

    private fun isCnh(texto: String): Boolean {
        val up = texto.uppercase()
        return up.contains("HABILITAÇÃO") ||
                up.contains("CARTEIRA NACIONAL") ||
                up.contains("DRIVER LICENSE") ||
                up.contains("PERMISO DE CONDUCCIÓN") ||
                up.contains("DETRAN") ||
                up.contains("MINISTÉRIO DA INFRAESTRUTURA") ||
                up.contains("SECRETARIA NACIONAL DE TRÂNSITO") ||
                up.contains("SENATRAN") ||
                up.contains("DENATRAN")
    }

    private fun isRg(texto: String): Boolean {
        val up = texto.uppercase()
        return up.contains("IDENTIDADE") ||
                up.contains("REGISTRO GERAL") ||
                up.contains("REPÚBLICA FEDERATIVA") ||
                (up.contains("SECRETARIA") && up.contains("SEGURANÇA"))
    }

    // ── Processamento CNH ─────────────────────────────────────────────────────

    /**
     * Estratégia baseada nos rótulos impressos na CNH brasileira:
     *
     *  Campo 2e1 → NOME E SOBRENOME
     *  Campo 1a  → 1ª HABILITAÇÃO
     *  Campo 3   → DATA, LOCAL E UF DE NASCIMENTO
     *  Campo 4a  → DATA EMISSÃO
     *  Campo 4b  → VALIDADE
     *  Campo 4c  → DOC. IDENTIDADE / ORG. EMISSOR / UF
     *  Campo 4d  → CPF
     *  Campo 5   → Nº REGISTRO
     *  Campo 9   → CAT. HAB.
     *  Campo ACC → (sufixo de categoria)
     */
    private fun processarCnh(linhas: List<String>, rawText: String): DadosCNH {

        // -- Nome (campo 2e1) -------------------------------------------------
        // Vem logo após linha que contém "NOME" e "SOBRENOME"
        val nome = extractLineAfterKeyword(linhas, listOf("NOME E SOBRENOME", "NOME SOBRENOME"))
            ?: extractNomeFallback(linhas)

        // -- 1ª Habilitação (campo 1a) ----------------------------------------
        val primeiraHab = extractDateAfterKeyword(linhas, listOf("1ª HABILITAÇÃO", "1a HABILITAÇÃO", "HABILITAÇÃO"))

        // -- Data e local de nascimento (campo 3) ------------------------------
        val linhaDataNasc = findLineContaining(linhas, listOf("DATA", "LOCAL", "NASCIMENTO"))
        val linhaConteudoNasc = linhaDataNasc?.let { getNextContentLine(linhas, it) }
        val dataNascimento = linhaConteudoNasc?.let { regexData.find(it)?.value }
            ?: regexData.findAll(rawText).map { it.value }.firstOrNull()
        val localNascimento = linhaConteudoNasc
            ?.replace(regexData, "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        // -- Data Emissão (campo 4a) ------------------------------------------
        val dataEmissao = extractDateAfterKeyword(linhas, listOf("DATA EMISSÃO", "EMISSÃO", "EMISSAO", "4a"))

        // -- Validade (campo 4b) -----------------------------------------------
        val dataValidade = extractDateAfterKeyword(linhas, listOf("VALIDADE", "4b"))

        // -- Doc. Identidade / RG + Órgão emissor (campo 4c) ------------------
        val linhaDoc = findLineContaining(linhas, listOf("DOC. IDENTIDADE", "DOC IDENTIDADE", "IDENTIDADE", "4c"))
        val linhaConteudoDoc = linhaDoc?.let { getNextContentLine(linhas, it) }
        val rg = linhaConteudoDoc?.let { regexRg.find(it)?.value }
            ?: extractRg(linhas, rawText)
        val orgaoEmissor = linhaConteudoDoc
            ?.replace(regexRg, "")
            ?.replace(Regex("""\d"""), "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        // -- CPF (campo 4d) ---------------------------------------------------
        val cpf = extractValueAfterKeyword(linhas, listOf("CPF", "4d"))
            ?.let { regexCpf.find(it)?.value?.formatCpf() }
            ?: regexCpf.find(rawText)?.value?.formatCpf()

        // -- Nº Registro (campo 5) --------------------------------------------
        val numeroRegistro = extractValueAfterKeyword(linhas, listOf("REGISTRO", "Nº REGISTRO", "N° REGISTRO", "5"))
            ?.let { regexRegistroCnh.find(it)?.value }
            ?: regexRegistroCnh.findAll(rawText)
                .map { it.value }
                .firstOrNull { it.length in 9..11 && it != cpf?.replace(Regex("""\D"""), "") }

        // -- Categoria (campo 9 / CAT. HAB.) ----------------------------------
        val categoria = extractValueAfterKeyword(linhas, listOf("CAT. HAB", "CAT HAB", "CATEGORIA", "9"))
            ?.trim()
            ?.uppercase()
            ?: linhas.firstOrNull { it.trim().matches(regexCategoria) }?.uppercase()

        // -- Filiação ---------------------------------------------------------
        val filiacao = extractLineAfterKeyword(linhas, listOf("FILIAÇÃO", "FILIACAO"))

        // -- Nacionalidade ----------------------------------------------------
        val nacionalidade = extractLineAfterKeyword(linhas, listOf("NACIONALIDADE"))

        return DadosCNH(
            nome = nome,
            cpf = cpf,
            rg = rg,
            dataNascimento = dataNascimento,
            localNascimento = localNascimento,
            numeroRegistro = numeroRegistro,
            primeiraHabilitacao = primeiraHab,
            dataEmissao = dataEmissao,
            dataValidade = dataValidade,
            categoria = categoria,
            orgaoEmissor = orgaoEmissor,
            filiacao = filiacao,
            nacionalidade = nacionalidade,
            rawText = rawText
        )
    }

    // ── Processamento RG ──────────────────────────────────────────────────────

    private fun processarRg(linhas: List<String>, rawText: String): DadosRG {
        val datas = regexData.findAll(rawText).map { it.value }.toList()
        val cpf = regexCpf.find(rawText)?.value?.formatCpf()
        val numeroRg = extractRg(linhas, rawText)

        val nome = extractLineAfterKeyword(linhas, listOf("NOME"))
            ?: extractNomeFallback(linhas)

        val nomeMae = extractLineAfterKeyword(linhas, listOf("MÃE", "MAE", "FILIAÇÃO", "FILIACAO"))
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

    // ── Helpers de extração ───────────────────────────────────────────────────

    /**
     * Retorna a PRÓXIMA linha de conteúdo após a linha-rótulo que contém
     * uma das [keywords]. Ignora linhas que parecem ser apenas rótulos.
     */
    private fun extractLineAfterKeyword(linhas: List<String>, keywords: List<String>): String? {
        val idx = linhas.indexOfFirst { line ->
            keywords.any { kw -> line.uppercase().contains(kw.uppercase()) }
        }
        if (idx < 0) return null
        // Pula linhas que são apenas rótulos (curtas, sem letras minúsculas úteis)
        for (i in (idx + 1) until linhas.size) {
            val candidate = linhas[i]
            if (candidate.isNotBlank() && !isLabelLine(candidate)) return candidate
        }
        return null
    }

    /**
     * Extrai a primeira data encontrada na linha de conteúdo após o rótulo.
     */
    private fun extractDateAfterKeyword(linhas: List<String>, keywords: List<String>): String? {
        // Tenta encontrar a data na mesma linha do rótulo primeiro
        val labelLine = linhas.firstOrNull { line ->
            keywords.any { kw -> line.uppercase().contains(kw.uppercase()) }
        }
        labelLine?.let { regexData.find(it)?.value }?.let { return it }

        // Depois tenta na próxima linha
        return extractLineAfterKeyword(linhas, keywords)?.let { regexData.find(it)?.value }
    }

    /**
     * Extrai o valor textual que aparece após o rótulo na MESMA linha
     * (ex: "CPF 076.763.758-51") ou na linha seguinte.
     */
    private fun extractValueAfterKeyword(linhas: List<String>, keywords: List<String>): String? {
        val line = linhas.firstOrNull { l ->
            keywords.any { kw -> l.uppercase().contains(kw.uppercase()) }
        } ?: return null

        // Valor na mesma linha após ":"
        val afterColon = line.substringAfter(":", "").trim()
        if (afterColon.isNotBlank()) return afterColon

        // Valor na mesma linha após o rótulo (separa por espaço)
        val keyword = keywords.first { kw -> line.uppercase().contains(kw.uppercase()) }
        val afterKeyword = line.uppercase().substringAfter(keyword).trim()
        if (afterKeyword.isNotBlank()) return afterKeyword

        // Linha seguinte
        return extractLineAfterKeyword(linhas, keywords)
    }

    /**
     * Localiza a linha-rótulo que contém uma das keywords.
     */
    private fun findLineContaining(linhas: List<String>, keywords: List<String>): String? =
        linhas.firstOrNull { line ->
            keywords.any { kw -> line.uppercase().contains(kw.uppercase()) }
        }

    /**
     * Retorna a próxima linha não-rótulo após [labelLine].
     */
    private fun getNextContentLine(linhas: List<String>, labelLine: String): String? {
        val idx = linhas.indexOf(labelLine)
        if (idx < 0) return null
        for (i in (idx + 1) until linhas.size) {
            if (!isLabelLine(linhas[i])) return linhas[i]
        }
        return null
    }

    /**
     * Heurística para identificar linhas que são apenas rótulos de campo
     * (ex: "4a DATA EMISSÃO", "NOME E SOBRENOME", "CAT. HAB.").
     */
    private fun isLabelLine(linha: String): Boolean {
        val up = linha.uppercase()
        val labelKeywords = listOf(
            "NOME", "SOBRENOME", "HABILITAÇÃO", "EMISSÃO", "VALIDADE",
            "REGISTRO", "IDENTIDADE", "CPF", "CATEGORIA", "NATURALIDADE",
            "FILIAÇÃO", "NACIONAL", "DATA", "LOCAL", "NASCIMENTO", "CAT"
        )
        // Linha com 3 ou menos palavras e contém palavra-chave de rótulo
        val palavras = up.trim().split(Regex("\\s+"))
        return palavras.size <= 4 && labelKeywords.any { up.contains(it) }
    }

    /**
     * Fallback para extrair nome: procura linha toda em maiúsculas,
     * com pelo menos 2 palavras, sem dígitos e sem palavras de rótulo.
     */
    private fun extractNomeFallback(linhas: List<String>): String? {
        val ignorar = setOf(
            "REPÚBLICA", "FEDERATIVA", "BRASIL", "HABILITAÇÃO", "DETRAN",
            "MINISTÉRIO", "INFRAESTRUTURA", "SECRETARIA", "SENATRAN",
            "DENATRAN", "CARTEIRA", "NACIONAL", "DRIVER", "LICENSE",
            "PERMISO", "CONDUCCIÓN", "BRASILEIRO", "BRASILEIRA"
        )
        return linhas.firstOrNull { linha ->
            val up = linha.uppercase()
            up == linha &&                              // toda maiúscula
                    linha.split(" ").size >= 2 &&       // ao menos 2 palavras
                    linha.length > 6 &&
                    linha.none { it.isDigit() } &&      // sem dígitos
                    ignorar.none { up.contains(it) }    // não é rótulo
        }
    }

    /**
     * Extrai número de RG, preferindo o padrão XX.XXX.XXX-X.
     */
    private fun extractRg(lines: List<String>, raw: String): String? {
        val afterLabel = extractValueAfterKeyword(
            lines, listOf("RG", "IDENTIDADE", "REGISTRO GERAL", "DOC. IDENTIDADE")
        )
        if (afterLabel != null) {
            regexRg.find(afterLabel)?.value?.let { return it }
        }
        return regexRg.find(raw)?.value
    }

    // ── Formatação ────────────────────────────────────────────────────────────

    private fun String.formatCpf(): String {
        val digits = replace(Regex("""\D"""), "")
        return if (digits.length == 11)
            "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9)}"
        else this
    }
}