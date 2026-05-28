package com.example.leitordocumento_compose.utils

import com.example.documentscan.DocumentType
import com.example.leitordocumento_compose.data.DadosCNH
import com.example.leitordocumento_compose.data.DadosRG
import com.example.leitordocumento_compose.data.OcrResultado
import com.google.mlkit.vision.text.Text


object DocumentoOcrProcessador {


    /** Data DD/MM/AAAA com separadores flexíveis */
    private val regexData = Regex("""(\d{2}[/.\-]\d{2}[/.\-]\d{4})""")

    /**
     * CPF — aceita formatado (000.000.000-00) e compacto (00000000000).
     * Exige 11 dígitos com separadores opcionais.
     */
    private val regexCpf = Regex("""(?<!\d)(\d{3}[.\- ]?\d{3}[.\- ]?\d{3}[.\- ]?\d{2})(?!\d)""")

    /** Nº registro CNH — 9 a 11 dígitos isolados */
    private val regexRegistroCnh = Regex("""(?<!\d)(\d{9,11})(?!\d)""")

    /**
     * RG — formatos comuns:
     *   XX.XXX.XXX-X  |  XX.XXX.XXX-X  |  XXXXXXXXX  (7-9 chars)
     */
    private val regexRg = Regex("""(?<!\d)(\d{1,2}\.?\d{3}\.?\d{3}-?[\dXx])(?!\d)""")

    /** Categoria de CNH — letras isoladas válidas */
    private val regexCategoriaExata = Regex(
        """(?<![A-Z0-9])([ABCDE]{1,2}(?:CC)?)(?![A-Z0-9])""",
        RegexOption.IGNORE_CASE
    )

    /** Valida se uma string limpa é uma categoria real */
    private val categoriasValidas = setOf(
        "A", "B", "C", "D", "E",
        "AB", "AC", "AD", "AE"
    )

    /** Rótulos de campo impressos nas CNHs brasileiras (novos e antigos modelos) */
    private val rotulosCategoria = listOf(
        "CAT. HAB", "CAT HAB", "CAT.HAB",
        "CAT. HABILITAÇÃO", "CAT HABILITAÇÃO",
        "CATEGORIA", "CAT.",
        "9"
    )

    // ── Ponto de entrada ──────────────────────────────────────────────────────

    fun processarDocumento(mlKitTexto: Text, tipoDocumentoSelecionado: DocumentType): OcrResultado {
        val textoCompleto = mlKitTexto.text
        val linhas = textoCompleto
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return when {
            isCnh(textoCompleto)  -> OcrResultado.Cnh(processarCnh(linhas, textoCompleto, mlKitTexto))
            isRg(textoCompleto)   -> OcrResultado.Rg(processarRg(linhas, textoCompleto))
            else                  -> OcrResultado.Desconhecido(rawText = textoCompleto)
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
    private fun processarCnh(linhas: List<String>, rawText: String, mlText: Text): DadosCNH {

        // ── Camada 1: extração por rótulo de linha ────────────────────────────

        // Nome (campo 2e1)
        val nome = extractLineAfterKeyword(linhas, listOf("NOME E SOBRENOME", "NOME SOBRENOME", "2E1", "2e1"))
            ?: extrairNomeFallBack(linhas)

        // 1ª Habilitação (campo 1a)
        val primeiraHab = extractDateAfterKeyword(linhas, listOf("1ª HABILITAÇÃO", "1a HABILITAÇÃO", "1A HABILITAÇÃO", "PRIMEIRA HABILITAÇÃO"))
            ?: extractDateAfterKeyword(linhas, listOf("1a", "1ª"))

        // Nascimento (campo 3)
        val (dataNascimento, localNascimento) = extrairNascimento(linhas, rawText)

        // Emissão (campo 4a)
        val dataEmissao = extractDateAfterKeyword(linhas, listOf("DATA EMISSÃO", "EMISSÃO", "EMISSAO", "4A", "4a"))

        // Validade (campo 4b)
        val dataValidade = extractDateAfterKeyword(linhas, listOf("VALIDADE", "4B", "4b", "VALID"))

        // Doc Identidade / RG / Órgão emissor (campo 4c)
        val (rg, orgaoEmissor) = extrairRgEOrgao(linhas, rawText)

        // CPF (campo 4d)
        val cpf = extrairCpf(linhas, rawText)

        // Nº Registro (campo 5)
        val numeroRegistro = extrairNumeroRegistro(linhas, rawText, cpf)

        // Categoria (campo 9)
        val categoria = extrairCategoria(linhas, rawText)

        // Filiação
        val filiacao = extractLineAfterKeyword(linhas, listOf("FILIAÇÃO", "FILIACAO"))

        // Nacionalidade
        val nacionalidade = extractLineAfterKeyword(linhas, listOf("NACIONALIDADE"))

        // ── Camada 3: reconciliação com datas ─────────────────────────────────
        // A CNH tem tipicamente 3-4 datas: nascimento, 1ªHab, emissão, validade.
        // Se alguma ficou nula, tenta preencher com as datas restantes no texto.
        val todasDatas = regexData.findAll(rawText).map { it.value }.toList()
        val datasUsadas = listOfNotNull(dataNascimento, primeiraHab, dataEmissao, dataValidade).toMutableList()

        fun proximaData(): String? = todasDatas.firstOrNull { it !in datasUsadas }
            ?.also { datasUsadas.add(it) }

        val dataNascFinal    = dataNascimento    ?: proximaData()
        val primeiraHabFinal = primeiraHab       ?: proximaData()
        val dataEmissaoFinal = dataEmissao       ?: proximaData()
        val dataValidadeFinal = dataValidade     ?: proximaData()

        return DadosCNH(
            nome              = nome,
            cpf               = cpf,
            rg                = rg,
            dataNascimento    = dataNascFinal,
            localNascimento   = localNascimento,
            numeroRegistro    = numeroRegistro,
            primeiraHabilitacao = primeiraHabFinal,
            dataEmissao       = dataEmissaoFinal,
            dataValidade      = dataValidadeFinal,
            categoria         = categoria,
            orgaoEmissor      = orgaoEmissor,
            filiacao          = filiacao,
            nacionalidade     = nacionalidade,
            rawText           = rawText
        )
    }

    private fun extrairNascimento(linhas: List<String>, rawText: String): Pair<String?, String?> {
        val rotulosNasc = listOf("DATA", "LOCAL", "NASCIMENTO", "3", "DATA NASC")
        val linhaRotulo = findLineContaining(linhas, rotulosNasc)
        val linhaConteudo = linhaRotulo?.let { getNextContentLine(linhas, it) }

        val data = linhaConteudo?.let { regexData.find(it)?.value }
        val local = linhaConteudo?.replace(regexData, "")?.trim()?.takeIf { it.isNotBlank() }

        return Pair(
            data ?: regexData.findAll(rawText).map { it.value }.firstOrNull(),
            local
        )
    }

    private fun extrairRgEOrgao(linhas: List<String>, rawText: String): Pair<String?, String?> {
        val rotulosDoc = listOf("DOC. IDENTIDADE", "DOC IDENTIDADE", "IDENTIDADE", "4C", "4c", "RG", "REGISTRO GERAL")
        val linhaDoc = findLineContaining(linhas, rotulosDoc)
        val conteudo = linhaDoc?.let { getNextContentLine(linhas, it) }

        val rg = conteudo?.let { regexRg.find(it)?.value }
            ?: extractValueAfterKeyword(linhas, listOf("RG", "REGISTRO GERAL", "DOC. IDENTIDADE"))
                ?.let { regexRg.find(it)?.value }
            ?: regexRg.find(rawText)?.value

        val orgao = conteudo
            ?.replace(regexRg, "")
            ?.replace(Regex("""\d"""), "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        return Pair(rg, orgao)
    }

    private fun extrairCpf(linhas: List<String>, rawText: String): String? {
        // Tenta pelo rótulo primeiro
        val porRotulo = extractValueAfterKeyword(linhas, listOf("CPF", "4D", "4d"))
            ?.let { regexCpf.find(it)?.value?.formatarCpf() }
        if (porRotulo != null) return porRotulo

        // Fallback: procura padrão de CPF no texto (11 dígitos com separadores)
        // Prefere a versão formatada se presente
        val formatado = Regex("""\d{3}\.\d{3}\.\d{3}-\d{2}""").find(rawText)?.value
        if (formatado != null) return formatado

        return regexCpf.find(rawText)?.value?.let { raw ->
            val digits = raw.replace(Regex("""\D"""), "")
            if (digits.length == 11) digits.formatarCpf() else null
        }
    }

    private fun extrairNumeroRegistro(linhas: List<String>, rawText: String, cpf: String?): String? {
        val cpfDigits = cpf?.replace(Regex("""\D"""), "") ?: ""

        val porRotulo = extractValueAfterKeyword(
            linhas, listOf("REGISTRO", "Nº REGISTRO", "N° REGISTRO", "N. REGISTRO", "5")
        )?.let { regexRegistroCnh.find(it)?.value }

        if (porRotulo != null && porRotulo != cpfDigits) return porRotulo

        // Fallback: primeiro número de 9-11 dígitos que não seja o CPF
        return regexRegistroCnh.findAll(rawText)
            .map { it.value }
            .firstOrNull { num ->
                num.length in 9..11 && num != cpfDigits
            }
    }

    // ── Processamento RG ──────────────────────────────────────────────────────

    private fun processarRg(linhas: List<String>, rawText: String): DadosRG {
        val datas = regexData.findAll(rawText).map { it.value }.toList()
        val cpf = extrairCpf(linhas, rawText)
        val numeroRg = extrairRgEOrgao(linhas, rawText).first

        val nome = extractLineAfterKeyword(linhas, listOf("NOME"))
            ?: extrairNomeFallBack(linhas)

        val nomeMae = extractLineAfterKeyword(linhas, listOf("MÃE", "MAE", "FILIAÇÃO", "FILIACAO"))
        val nomePai = extractLineAfterKeyword(linhas, listOf("PAI"))
        val naturalidade = extractLineAfterKeyword(linhas, listOf("NATURALIDADE", "NATURAL"))

        return DadosRG(
            nome           = nome,
            rg             = numeroRg,
            cpf            = cpf,
            dataNascimento = datas.firstOrNull(),
            nomeMae        = nomeMae,
            nomePai        = nomePai,
            naturalidade   = naturalidade,
            dataEmissao    = datas.lastOrNull(),
            rawText        = rawText
        )
    }

    // ── Helpers de extração ───────────────────────────────────────────────────


    // ── Nova função de extração de categoria ─────────────────────────────────────

    /**
     * Extrai a categoria da CNH com múltiplas estratégias, cobrindo
     * modelos antigos e novos.
     *
     * Ordem de prioridade:
     *  1. Valor na mesma linha do rótulo (ex: "CAT. HAB. B", "CATEGORIA: AB")
     *  2. Linha imediatamente após o rótulo
     *  3. Linha após rótulo + 1 (MLKit às vezes pula uma linha vazia)
     *  4. Fallback: procura linha que seja exclusivamente uma categoria válida
     *  5. Último recurso: varre o rawText inteiro buscando padrão isolado
     */
    private fun extrairCategoria(linhas: List<String>, rawText: String): String? {

        // Estratégia 1 e 2: pelo rótulo
        for (rotulo in rotulosCategoria) {
            val idxRotulo = linhas.indexOfFirst { linha ->
                linha.uppercase().contains(rotulo.uppercase())
            }
            if (idxRotulo < 0) continue

            // Tenta extrair da própria linha do rótulo
            val mesmaLinha = linhas[idxRotulo]
            val categoriaInline = extrairCategoriaDeTexto(
                mesmaLinha.uppercase().substringAfter(rotulo.uppercase()).trim()
            )
            if (categoriaInline != null) return categoriaInline

            // Tenta nas próximas 3 linhas (MLKit pode inserir quebras extras)
            for (offset in 1..3) {
                val idx = idxRotulo + offset
                if (idx >= linhas.size) break
                val candidato = linhas[idx].trim()
                val cat = extrairCategoriaDeTexto(candidato)
                if (cat != null) return cat
                // Para se encontrou uma linha longa que claramente não é categoria
                if (candidato.length > 10 && candidato.none { it.isDigit() }) break
            }
        }

        // Estratégia 3: linha isolada que seja exatamente uma categoria válida
        linhas.forEach { linha ->
            val up = linha.trim().uppercase()
            if (up in categoriasValidas) return up
        }

        // Estratégia 4: varredura do rawText com regex isolado
        // Útil quando o MLKit colapsa campos em uma única linha
        regexCategoriaExata.findAll(rawText.uppercase()).forEach { match ->
            val valor = match.groupValues[1].uppercase()
            if (valor in categoriasValidas) {
                // Confirma que está próximo de um rótulo de categoria no texto
                val posicao = match.range.first
                val janela = rawText.uppercase().substring(
                    maxOf(0, posicao - 60),
                    minOf(rawText.length, posicao + 60)
                )
                if (rotulosCategoria.any { janela.contains(it.uppercase()) }) {
                    return valor
                }
            }
        }

        // Estratégia 5: último recurso sem validar proximidade
        regexCategoriaExata.findAll(rawText.uppercase()).forEach { match ->
            val valor = match.groupValues[1].uppercase()
            if (valor in categoriasValidas && valor.length >= 1) return valor
        }

        return null
    }

    /**
     * Tenta extrair uma categoria válida de um fragmento de texto.
     * Ex: "B", "AB", " ACC ", "B (ACC)", "CAT B" → "B", "AB", "ACC"
     */
    private fun extrairCategoriaDeTexto(texto: String): String? {
        if (texto.isBlank()) return null

        // Testa se o texto inteiro (limpo) é uma categoria
        val limpo = texto.trim().uppercase()
            .replace(Regex("""[():\-\s]+"""), " ")
            .trim()

        if (limpo in categoriasValidas) return limpo

        // Procura a primeira ocorrência de categoria válida isolada
        val match = regexCategoriaExata.find(limpo) ?: return null
        val candidato = match.groupValues[1].uppercase()
        return candidato.takeIf { it in categoriasValidas }
    }



    /**
     * Retorna a primeira linha de conteúdo após a linha que contém uma keyword.
     * Ignora linhas que parecem ser apenas rótulos.
     */
    private fun extractLineAfterKeyword(linhas: List<String>, keywords: List<String>): String? {
        val idx = linhas.indexOfFirst { line ->
            keywords.any { kw -> line.uppercase().contains(kw.uppercase()) }
        }
        if (idx < 0) return null
        for (i in (idx + 1) until linhas.size) {
            val candidate = linhas[i]
            if (candidate.isNotBlank() && !isLinhaLabel(candidate)) return candidate
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

        // Valor após ":" na linha original (não uppercase)
        val afterColon = line.substringAfter(":", "").trim()
        if (afterColon.isNotBlank()) return afterColon

        // Valor após o rótulo na linha original
        val keyword = keywords.first { kw -> line.uppercase().contains(kw.uppercase()) }
        val startIdx = line.uppercase().indexOf(keyword.uppercase()) + keyword.length
        val afterKeyword = line.substring(startIdx).trim()
        if (afterKeyword.isNotBlank()) return afterKeyword

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
            if (!isLinhaLabel(linhas[i])) return linhas[i]
        }
        return null
    }

    /**
     * Heurística para identificar linhas que são apenas rótulos de campo
     * (ex: "4a DATA EMISSÃO", "NOME E SOBRENOME", "CAT. HAB.").
     */
    private fun isLinhaLabel(linha: String): Boolean {
        val up = linha.uppercase()
        val labelKeywords = listOf(
            "NOME", "SOBRENOME", "HABILITAÇÃO", "EMISSÃO", "VALIDADE",
            "REGISTRO", "IDENTIDADE", "CPF", "CATEGORIA", "NATURALIDADE",
            "FILIAÇÃO", "NACIONAL", "DATA", "LOCAL", "NASCIMENTO", "CAT",
            "ADMINISTRAÇÃO", "CONDUÇÃO", "SECRETARIA", "DETRAN"
        )
        val palavras = up.trim().split(Regex("\\s+"))
        return palavras.size <= 5 && labelKeywords.any { up.contains(it) }
    }

    /**
     * Fallback para extrair nome: procura linha toda em maiúsculas,
     * com pelo menos 2 palavras, sem dígitos e sem palavras de rótulo.
     */
    private fun extrairNomeFallBack(linhas: List<String>): String? {
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

    private fun String.formatarCpf(): String {
        val digits = replace(Regex("""\D"""), "")
        return if (digits.length == 11)
            "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${
                digits.substring(
                    6,
                    9
                )
            }-${digits.substring(9)}"
        else this
    }
}