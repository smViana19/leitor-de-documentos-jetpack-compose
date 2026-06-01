package com.example.leitordocumento_compose.utils

import com.example.documentscan.DocumentType
import com.example.leitordocumento_compose.data.DadosCNH
import com.example.leitordocumento_compose.data.DadosCRLV
import com.example.leitordocumento_compose.data.DadosRG
import com.example.leitordocumento_compose.data.OcrResultado
import com.google.mlkit.vision.text.Text


object DocumentoOcrProcessador
{

    // ── Regex compartilhadas ──────────────────────────────────────────────────

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
    private val regexRg = Regex("""M?[\d][.\d\-X]{4,19}""", RegexOption.IGNORE_CASE)

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

    private val CORES_OFICIAIS = setOf(
        "AMARELA", "AZUL", "BEGE", "BRANCA", "CINZA", "DOURADA", "GRENA", "LARANJA",
        "MARROM", "PRATA", "PRETA", "ROSA", "ROXA", "VERDE", "VERMELHA", "FANTASIA"
    )

    private val CATEGORIAS_OFICIAIS = setOf(
        "PARTICULAR", "ALUGUEL", "OFICIAL", "APRENDIZAGEM",
        "COLECÃO", "COLECAO", "DIPLOMATICO", "EXPERIENCIA", "FABRICANTE"
    )

    private val regexApenasLetrasECat = Regex("[^A-ZÁÉÍÓÚÂÊÔÇ ]") // Limpa sujeiras do OCR

    private val regexApenasLetras = Regex("[^A-ZÁÉÍÓÚÂÊÔÇ ]") // Remove tudo que não for letra ou espaço

    // ── Regex específicas para CRLV ───────────────────────────────────────────

    /**
     * Placa Mercosul: ABC1D23
     * Placa antiga: ABC-1234 ou ABC1234
     */
    private val regexPlaca = Regex(
        """(?<![A-Z0-9])([A-Z]{3}[- ]?\d[A-Z0-9]\d{2})(?![A-Z0-9])""",
        RegexOption.IGNORE_CASE
    )

    /**
     * RENAVAM: 9 ou 11 dígitos.
     * Evita confusão com chassi (17 chars) e outros números.
     */
    private val regexRenavam = Regex("""(?<!\d)(\d{9}|\d{11})(?!\d)""")

    /**
     * VIN / Chassi: 17 caracteres alfanuméricos (excluindo I, O, Q para evitar ambiguidade).
     */
    private val regexChassi = Regex(
        """(?<![A-Z0-9])([A-HJ-NPR-Z0-9]{17})(?![A-Z0-9])""",
        RegexOption.IGNORE_CASE
    )

    /** Ano isolado de 4 dígitos (1950–2099) */
    private val regexAno = Regex("""(?<!\d)((?:19[5-9]\d|20\d{2}))(?!\d)""")

    /** Validade de licenciamento: MM/AAAA */
    private val regexValidadeCrlv = Regex("""(\d{2}[/]\d{4})""")

    // ── Ponto de entrada ──────────────────────────────────────────────────────

    fun processarDocumento(mlKitTexto: Text, tipoDocumentoSelecionado: DocumentType): OcrResultado
    {
        val textoCompleto = mlKitTexto.text
        val linhas = textoCompleto
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return when
        {
            isCrlv(textoCompleto) -> OcrResultado.Crlv(processarCrlv(linhas, textoCompleto))
            isCnh(textoCompleto) -> OcrResultado.Cnh(processarCnh(linhas,
                textoCompleto,
                mlKitTexto))
            isRg(textoCompleto) -> OcrResultado.Rg(processarRg(linhas, textoCompleto))
            else -> OcrResultado.Desconhecido(rawText = textoCompleto)
        }
    }

    // ── Detecção de tipo ──────────────────────────────────────────────────────

    private fun isCrlv(texto: String): Boolean
    {
        val up = texto.uppercase()
        var score = 0

        // Termos exclusivos do CRLV
        if (up.contains("CRLV")) score += 5
        if (up.contains("LICENCIAMENTO")) score += 3
        if (up.contains("RENAVAM")) score += 4
        if (up.contains("CHASSI")) score += 3
        if (up.contains("REGISTRO E LICENCIAMENTO")) score += 5
        if (up.contains("CERTIFICADO DE REGISTRO")) score += 5

        // Termos de apoio
        if (up.contains("RENAVAM")) score += 2
        if (up.contains("MARCA/MODELO") || up.contains("MARCA / MODELO")) score += 2
        if (up.contains("ESPÉCIE") || up.contains("ESPECIE")) score += 2
        if (up.contains("COMBUSTÍVEL") || up.contains("COMBUSTIVEL")) score += 2
        if (up.contains("DENATRAN") || up.contains("SENATRAN")) score += 1
        if (up.contains("EXERCÍCIO") || up.contains("EXERCICIO")) score += 2

        // Termos que indicam que NÃO é CRLV
        if (up.contains("HABILITAÇÃO")) score -= 5   // CNH
        if (up.contains("IDENTIDADE")) score -= 5   // RG

        return score >= 4
    }

    private fun isCnh(texto: String): Boolean
    {
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

    private fun isRg(texto: String): Boolean
    {
        val up = texto.uppercase()
        return up.contains("IDENTIDADE") ||
                up.contains("REGISTRO GERAL") ||
                up.contains("REPÚBLICA FEDERATIVA") ||
                (up.contains("SECRETARIA") && up.contains("SEGURANÇA"))
    }

    // ── Processamento CRLV ────────────────────────────────────────────────────

    /**
     * Extrai campos do CRLV usando rótulos impressos no documento.
     *
     * Layout típico (modelos 2010–2024):
     *  - RENAVAM
     *  - PLACA / MUNICÍPIO-UF
     *  - MARCA/MODELO/VERSÃO
     *  - ANO FAB. / ANO MOD.
     *  - CHASSI
     *  - COR PREDOMINANTE / COMBUSTÍVEL
     *  - ESPÉCIE/TIPO / CATEGORIA
     *  - PROPRIETÁRIO
     *  - CPF/CNPJ
     *  - EXERCÍCIO / VALIDADE
     */
    private fun processarCrlv(linhas: List<String>, rawText: String): DadosCRLV
    {

        val placa = extrairPlaca(linhas, rawText)

        val renavam = extractValueAfterKeyword(linhas, listOf("RENAVAM"))
            ?.let { regexRenavam.find(it.replace(".", "").replace("-", ""))?.value }
            ?: regexRenavam.findAll(rawText)
                .map { it.value }
                .firstOrNull { it.length in setOf(9, 11) && it != placa?.replace("-", "") }

        val chassi = extrairChassi(linhas, rawText)

        val proprietario = extractLineAfterKeyword(
            linhas, listOf("PROPRIETÁRIO", "PROPRIETARIO", "NOME DO PROPRIETÁRIO", "NOME")
        )?.takeIf { it.none { c -> c.isDigit() } || it.length > 8 }

        val cpfCnpj = extrairCpfOuCnpj(linhas, rawText)

        val (marca, modelo) = extrairMarcaModelo(linhas, rawText)

        val (anoFab, anoMod) = extrairAnos(linhas, rawText)

        val cor = extrairCor(linhas)

        val combustivel = extractValueAfterKeyword(
            linhas, listOf("COMBUSTÍVEL", "COMBUSTIVEL", "COMB.")
        )?.trim()?.uppercase()

        val especie = extractValueAfterKeyword(
            linhas, listOf("ESPÉCIE", "ESPECIE", "ESPÉCIE/TIPO")
        )?.trim()?.uppercase()

        val tipo = extractValueAfterKeyword(
            linhas, listOf("TIPO", "CARROCERIA")
        )?.trim()?.uppercase()


        // No CRLV é "PARTICULAR", "ALUGUEL", etc. diferente da CNH
        val categoriaVeiculo = extractValueAfterKeyword(
            linhas, listOf("CATEGORIA", "CAT.")
        )?.trim()?.uppercase()
            ?.takeIf { it.length in 3..20 }

        val (municipio, uf) = extrairMunicipioUf(linhas, rawText, placa)


        val exercicio = extractValueAfterKeyword(
            linhas, listOf("EXERCÍCIO", "EXERCICIO", "ANO EXERC", "EXERC.")
        )?.let { regexAno.find(it)?.value }

        val validade = extractValueAfterKeyword(
            linhas, listOf("VALIDADE", "VÁLIDO ATÉ", "VENCIMENTO")
        )?.let {
            regexValidadeCrlv.find(it)?.value ?: regexData.find(it)?.value
        } ?: regexValidadeCrlv.find(rawText)?.value

        // ── Dados técnicos opcionais ──────────────────────────────────────────
        val potencia = extractValueAfterKeyword(linhas, listOf("POTÊNCIA", "POTENCIA", "CV"))
            ?.let { Regex("""\d+""").find(it)?.value }
        val cilindrada = extractValueAfterKeyword(linhas, listOf("CILINDRADA", "CC"))
            ?.let { Regex("""\d+""").find(it)?.value }
        val pbt = extractValueAfterKeyword(linhas, listOf("PBT", "PESO BRUTO"))
            ?.let { Regex("""\d+""").find(it)?.value }
        val cmt = extractValueAfterKeyword(linhas, listOf("CMT", "CAPACIDADE MÁXIMA"))
            ?.let { Regex("""\d+""").find(it)?.value }
        val capacidade = extractValueAfterKeyword(linhas, listOf("LOTAÇÃO", "PASSAGEIROS", "CAP."))
            ?.let { Regex("""\d+""").find(it)?.value }

        return DadosCRLV(
            placa = placa,
            renavam = renavam,
            chassi = chassi,
            proprietario = proprietario,
            cpfCnpjProprietario = cpfCnpj,
            marca = marca,
            modelo = modelo,
            anoFabricacao = anoFab,
            anoModelo = anoMod,
            corPredominante = cor,
            combustivel = combustivel,
            especie = especie,
            tipo = tipo,
            categoria = categoriaVeiculo,
            municipio = municipio,
            uf = uf,
            validade = validade,
            exercicio = exercicio,
            potencia = potencia,
            cilindrada = cilindrada,
            pbt = pbt,
            cmt = cmt,
            capacidade = capacidade,
            rawText = rawText
        )
    }


    /**
     * Extrai a placa do veículo, suportando formato Mercosul e antigo.
     * Tenta primeiro pelo rótulo e depois varre o texto completo.
     */
    private fun extrairPlaca(linhas: List<String>, rawText: String): String?
    {
        // Por rótulo
        val porRotulo = extractValueAfterKeyword(linhas, listOf("PLACA", "PLACA DO VEÍCULO"))
            ?.let { regexPlaca.find(it)?.value?.uppercase() }
        if (porRotulo != null) return porRotulo

        // Varredura — pega a primeira ocorrência que parece placa de carro
        return regexPlaca.find(rawText.uppercase())?.value
            ?.replace(" ", "")
            ?.replace("-", "")
            ?.let { raw ->
                // Formata: se Mercosul (7 chars), devolve sem hífen; se antigo, ABC-1234
                if (raw.length == 7 && raw[3].isDigit() && raw[4].isLetter()) raw  // Mercosul
                else if (raw.length == 7) "${raw.substring(0, 3)}-${raw.substring(3)}" // Antigo
                else raw
            }
    }

    /**
     * Extrai o chassi (VIN de 17 chars).
     * O OCR pode trocar 'O' por '0', 'I' por '1' — faz normalização básica.
     */
    private fun extrairChassi(linhas: List<String>, rawText: String): String?
    {
        // Função interna rápida para limpar sujeiras (tira espaços, hifens, pontos)
        fun limparOcr(texto: String): String
        {
            return texto.uppercase()
                .replace(Regex("[^A-Z0-9]"), "") // Remove tudo que não for letra ou número
                .replace("O",
                    "0")               // Corrige erro crônico de OCR (Letra O -> Número 0)
                .replace("I",
                    "1")               // Corrige erro crônico de OCR (Letra I -> Número 1)
                .replace("Q", "0")               // Corrige erro de OCR (Letra Q -> Número 0)
        }

        // 1. Busca por rótulo
        val valorRotulo = extractValueAfterKeyword(
            linhas, listOf("CHASSI", "NÚM. DO CHASSI", "NUMERO DO CHASSI", "N° CHASSI", "CHASSIS")
        )

        if (valorRotulo != null)
        {
            val trechoLimpo = limparOcr(valorRotulo)
            val match = regexChassi.find(trechoLimpo)
            if (match != null) return match.value
        }

        // 2. Fallback no texto completo (rawText)
        // Limpamos o rawText inteiro. Isso resolve o problema de o OCR ler o chassi
        // quebrado em pedaços com espaços no meio (ex: "9 B W Z Z Z ...")
        val rawTextLimpo = limparOcr(rawText)

        return regexChassi.find(rawTextLimpo)?.value
    }

    /**
     * Extrai marca e modelo do CRLV.
     * O campo geralmente aparece como "MARCA/MODELO/VERSÃO" em uma linha
     * com o valor logo abaixo (ex: "VOLKSWAGEN / GOL 1.0 CITY FLEX 4P").
     */
    private fun extrairMarcaModelo(linhas: List<String>, rawText: String): Pair<String?, String?>
    {
        val rotulosMarca = listOf(
            "MARCA/MODELO/VERSÃO", "MARCA/MODELO", "MARCA / MODELO",
            "MARCA", "FABRICANTE"
        )
        val linha = extractLineAfterKeyword(linhas, rotulosMarca) ?: return Pair(null, null)

        val partes = linha.split(Regex("""\s*/\s*"""), limit = 2)
        return if (partes.size >= 2)
        {
            Pair(partes[0].trim().uppercase(), partes[1].trim().uppercase())
        }
        else
        {
            // Tenta separar pela primeira palavra (marca) do resto (modelo)
            val palavras = linha.trim().uppercase().split(" ", limit = 2)
            Pair(palavras.firstOrNull(), palavras.getOrNull(1))
        }
    }

    fun extrairCor(linhas: List<String>): String?
    {
        val valorExtraido = extractValueAfterKeyword(
            linhas, listOf("COR PREDOMINANTE", "COR PRED", "COR")
        )?.uppercase()?.trim() ?: return null

        val textoLimpo = valorExtraido
            .replace(regexApenasLetras, "")
            .trim()

        if (textoLimpo.isBlank()) return null

        val corEncontrada = CORES_OFICIAIS.firstOrNull { corOficial ->
            textoLimpo.contains(corOficial) ||
                    (corOficial.length > 4 && textoLimpo.startsWith(corOficial.substring(0,
                        corOficial.length - 1)))
        }

        if (corEncontrada != null)
        {
            return corEncontrada
        }

        val primeiraPalavra = textoLimpo.split(Regex("\\s+")).firstOrNull()
        return primeiraPalavra?.takeIf { it.length >= 3 }

    }

    fun extrairCategoria(linhas: List<String>): String?
    {
        val valorExtraido = extractValueAfterKeyword(
            linhas, listOf("CATEGORIA", "CAT.")
        )?.uppercase()?.trim() ?: return null
        val textoLimpo = valorExtraido
            .replace(regexApenasLetrasECat, "")
            .trim()

        if (textoLimpo.isBlank()) return null

        val categoriaEncontrada = CATEGORIAS_OFICIAIS.firstOrNull { catOficial ->
            textoLimpo.contains(catOficial)
        }

        if (categoriaEncontrada != null)
        {
            // Retorna o padrão corrigido sem acento se preferir, ou o encontrado
            return if (categoriaEncontrada == "COLECAO") "COLEÇÃO" else categoriaEncontrada
        }

        val primeiraPalavra = textoLimpo.split(Regex("\\s+")).firstOrNull() ?: ""
        if (primeiraPalavra.length >= 4)
        {
            return primeiraPalavra
        }

        return null
    }

    /**
     * Extrai ano de fabricação e ano do modelo.
     * O CRLV geralmente exibe "ANO FAB. ANO MOD." na mesma linha ou em linhas separadas.
     */
    private fun extrairAnos(linhas: List<String>, rawText: String): Pair<String?, String?>
    {
        val linhasFiltradas = linhas.filterNot { linha ->
            val up = linha.uppercase()
            up.contains("EXERC") // Ignora "EXERCÍCIO", "EXERC.", "EXERCICIO"
        }

        val linhaCombinada = linhasFiltradas.firstOrNull { linha ->
            val up = linha.uppercase()
            (up.contains("ANO") || up.contains("FAB")) &&
                    Regex("""\d{4}\s*/\s*\d{4}""").containsMatchIn(linha)
        }
        if (linhaCombinada != null)
        {
            val anos = Regex("""\d{4}""").findAll(linhaCombinada).map { it.value }.toList()
            if (anos.size >= 2) return Pair(anos[0], anos[1])
        }

        val anoFabricacao = extractValueAfterKeyword(
            linhasFiltradas, listOf("ANO FAB", "ANO DE FABRICAÇÃO", "ANO FABRICAÇÃO", "ANO FAB.")
        )?.let { regexAno.find(it)?.value }

        val anoModelo = extractValueAfterKeyword(
            linhasFiltradas, listOf("ANO MOD", "ANO DO MODELO", "ANO MODELO", "ANO MOD.")
        )?.let { regexAno.find(it)?.value }

        if (anoFabricacao == null && anoModelo == null)
        {
            // Remove a linha do exercício do rawText antes de aplicar o regex genérico
            val rawTextSemExercicio = rawText.lines()
                .filterNot { it.uppercase().contains("EXERC") }
                .joinToString("\n")

            val anos = regexAno.findAll(rawTextSemExercicio).map { it.value }.distinct().take(2)
                .toList()
            return Pair(anos.getOrNull(0), anos.getOrNull(1))
        }

        return Pair(anoFabricacao, anoModelo)
    }

    /**
     * Extrai município e UF do CRLV.
     * Exemplo: "SÃO PAULO - SP" ou "MUNICÍPIO: CAMPINAS UF: SP"
     */
    private fun extrairMunicipioUf(
        linhas: List<String>,
        rawText: String,
        placa: String?
    ): Pair<String?, String?>
    {
        // Por rótulo
        val linhaMunicipio = extractLineAfterKeyword(
            linhas, listOf("LOCAL")
        )

        if (linhaMunicipio != null)
        {
            // Tenta separar "CIDADE - UF"
            val match = Regex("""^(.+?)\s*[-–]\s*([A-Z]{2})$""")
                .find(linhaMunicipio.trim().uppercase())
            if (match != null)
            {
                return Pair(match.groupValues[1].trim(), match.groupValues[2])
            }
            // Extrai UF (2 letras no final)
            val uf = Regex("""\b([A-Z]{2})\b""").findAll(linhaMunicipio.uppercase())
                .lastOrNull()?.value
            return Pair(linhaMunicipio.replace(uf ?: "", "").trim().uppercase(), uf)
        }

        // Fallback: linha na mesma posição que a placa (CRLV antigo)
        return Pair(null, null)
    }

    /**
     * Extrai CPF (11 dígitos) ou CNPJ (14 dígitos) do proprietário.
     */
    private fun extrairCpfOuCnpj(linhas: List<String>, rawText: String): String?
    {
        val regexCnpj = Regex("""(?<!\d)(\d{2}\.?\d{3}\.?\d{3}/?\d{4}-?\d{2})(?!\d)""")

        // Por rótulo de CPF
        val cpfPorRotulo = extractValueAfterKeyword(linhas, listOf("CPF", "CPF/CNPJ"))
            ?.let { regexCpf.find(it)?.value?.formatarCpf() }
        if (cpfPorRotulo != null) return cpfPorRotulo

        val cnpjPorRotulo = extractValueAfterKeyword(linhas, listOf("CNPJ"))
            ?.let { regexCnpj.find(it)?.value }
        if (cnpjPorRotulo != null) return cnpjPorRotulo

        regexCnpj.find(rawText)?.value?.let { return it }

        return regexCpf.find(rawText)?.value?.formatarCpf()
    }

    // ── Processamento CNH ─────────────────────────────────────────────────────

    private fun processarCnh(linhas: List<String>, rawText: String, mlText: Text): DadosCNH
    {

        val nome = extractLineAfterKeyword(linhas,
            listOf("NOME E SOBRENOME", "NOME SOBRENOME", "2E1", "2e1"))
            ?: extrairNomeFallBack(linhas)

        val primeiraHab = extractDateAfterKeyword(linhas,
            listOf("1ª HABILITAÇÃO", "1a HABILITAÇÃO", "1A HABILITAÇÃO", "PRIMEIRA HABILITAÇÃO"))
            ?: extractDateAfterKeyword(linhas, listOf("1a", "1ª"))

        val (dataNascimento, localNascimento) = extrairNascimento(linhas, rawText)
        val dataEmissao = extractDateAfterKeyword(linhas,
            listOf("DATA EMISSÃO", "EMISSÃO", "EMISSAO", "4A", "4a"))
        val dataValidade = extractDateAfterKeyword(linhas, listOf("VALIDADE", "4B", "4b", "VALID"))
        val (rg, orgaoEmissor) = extrairRgEOrgao(linhas, rawText)
        val cpf = extrairCpf(linhas, rawText)
        val numeroRegistro = extrairNumeroRegistro(linhas, rawText, cpf)
        val categoria = extrairCategoria(linhas, rawText)
        val filiacao = extractLineAfterKeyword(linhas, listOf("FILIAÇÃO", "FILIACAO"))
        val nacionalidade = extractLineAfterKeyword(linhas, listOf("NACIONALIDADE"))

        val todasDatas = regexData.findAll(rawText).map { it.value }.toList()
        val datasUsadas = listOfNotNull(dataNascimento,
            primeiraHab,
            dataEmissao,
            dataValidade).toMutableList()

        fun proximaData(): String? = todasDatas.firstOrNull { it !in datasUsadas }
            ?.also { datasUsadas.add(it) }

        return DadosCNH(
            nome = nome,
            cpf = cpf,
            rg = rg,
            dataNascimento = dataNascimento ?: proximaData(),
            localNascimento = localNascimento,
            numeroRegistro = numeroRegistro,
            primeiraHabilitacao = primeiraHab ?: proximaData(),
            dataEmissao = dataEmissao ?: proximaData(),
            dataValidade = dataValidade ?: proximaData(),
            categoria = categoria,
            orgaoEmissor = orgaoEmissor,
            filiacao = filiacao,
            nacionalidade = nacionalidade,
            rawText = rawText
        )
    }

    private fun extrairNascimento(linhas: List<String>, rawText: String): Pair<String?, String?>
    {
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

    private fun extrairRgEOrgao(linhas: List<String>, rawText: String): Pair<String?, String?>
    {
        val rotulosDoc = listOf("DOC. IDENTIDADE",
            "DOC IDENTIDADE",
            "IDENTIDADE",
            "4C",
            "4c",
            "RG",
            "REGISTRO GERAL")
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

    private fun extrairCpf(linhas: List<String>, rawText: String): String?
    {
        val porRotulo = extractValueAfterKeyword(linhas, listOf("CPF", "4D", "4d"))
            ?.let { regexCpf.find(it)?.value?.formatarCpf() }
        if (porRotulo != null) return porRotulo

        val formatado = Regex("""\d{3}\.\d{3}\.\d{3}-\d{2}""").find(rawText)?.value
        if (formatado != null) return formatado

        return regexCpf.find(rawText)?.value?.let { raw ->
            val digits = raw.replace(Regex("""\D"""), "")
            if (digits.length == 11) digits.formatarCpf() else null
        }
    }

    private fun extrairNumeroRegistro(linhas: List<String>, rawText: String, cpf: String?): String?
    {
        val cpfDigits = cpf?.replace(Regex("""\D"""), "") ?: ""

        val porRotulo = extractValueAfterKeyword(
            linhas, listOf("REGISTRO", "Nº REGISTRO", "N° REGISTRO", "N. REGISTRO", "5")
        )?.let { regexRegistroCnh.find(it)?.value }

        if (porRotulo != null && porRotulo != cpfDigits) return porRotulo

        return regexRegistroCnh.findAll(rawText)
            .map { it.value }
            .firstOrNull { num -> num.length in 9..11 && num != cpfDigits }
    }

    // ── Processamento RG ──────────────────────────────────────────────────────

    private fun processarRg(linhas: List<String>, rawText: String): DadosRG
    {
        val datas = regexData.findAll(rawText).map { it.value }.toList()
        val cpf = extrairCpf(linhas, rawText)
        val numeroRg = extrairRgEOrgao(linhas, rawText).first

        val nome = extractLineAfterKeyword(linhas, listOf("NOME"))
            ?: extrairNomeFallBack(linhas)

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


    private fun extrairCategoria(linhas: List<String>, rawText: String): String?
    {

        for (rotulo in rotulosCategoria)
        {
            val idxRotulo = linhas.indexOfFirst { linha ->
                linha.uppercase().contains(rotulo.uppercase())
            }
            if (idxRotulo < 0) continue

            val mesmaLinha = linhas[idxRotulo]
            val categoriaInline = extrairCategoriaDeTexto(
                mesmaLinha.uppercase().substringAfter(rotulo.uppercase()).trim()
            )
            if (categoriaInline != null) return categoriaInline

            for (offset in 1..3)
            {
                val idx = idxRotulo + offset
                if (idx >= linhas.size) break
                val candidato = linhas[idx].trim()
                val cat = extrairCategoriaDeTexto(candidato)
                if (cat != null) return cat
                if (candidato.length > 10 && candidato.none { it.isDigit() }) break
            }
        }

        linhas.forEach { linha ->
            val up = linha.trim().uppercase()
            if (up in categoriasValidas) return up
        }

        regexCategoriaExata.findAll(rawText.uppercase()).forEach { match ->
            val valor = match.groupValues[1].uppercase()
            if (valor in categoriasValidas)
            {
                val posicao = match.range.first
                val janela = rawText.uppercase().substring(
                    maxOf(0, posicao - 60),
                    minOf(rawText.length, posicao + 60)
                )
                if (rotulosCategoria.any { janela.contains(it.uppercase()) })
                {
                    return valor
                }
            }
        }

        regexCategoriaExata.findAll(rawText.uppercase()).forEach { match ->
            val valor = match.groupValues[1].uppercase()
            if (valor in categoriasValidas && valor.length >= 1) return valor
        }

        return null
    }

    private fun extrairCategoriaDeTexto(texto: String): String?
    {
        if (texto.isBlank()) return null
        val limpo = texto.trim().uppercase()
            .replace(Regex("""[():\-\s]+"""), " ")
            .trim()
        if (limpo in categoriasValidas) return limpo
        val match = regexCategoriaExata.find(limpo) ?: return null
        val candidato = match.groupValues[1].uppercase()
        return candidato.takeIf { it in categoriasValidas }
    }

    private fun extractLineAfterKeyword(linhas: List<String>, keywords: List<String>): String?
    {
        val idx = linhas.indexOfFirst { line ->
            keywords.any { kw -> line.uppercase().contains(kw.uppercase()) }
        }
        if (idx < 0) return null
        for (i in (idx + 1) until linhas.size)
        {
            val candidate = linhas[i]
            if (candidate.isNotBlank() && !isLinhaLabel(candidate)) return candidate
        }
        return null
    }

    private fun extractDateAfterKeyword(linhas: List<String>, keywords: List<String>): String?
    {
        val labelLine = linhas.firstOrNull { line ->
            keywords.any { kw -> line.uppercase().contains(kw.uppercase()) }
        }
        labelLine?.let { regexData.find(it)?.value }?.let { return it }
        return extractLineAfterKeyword(linhas, keywords)?.let { regexData.find(it)?.value }
    }

    private fun extractValueAfterKeyword(linhas: List<String>, keywords: List<String>): String?
    {
        val line = linhas.firstOrNull { l ->
            keywords.any { kw -> l.uppercase().contains(kw.uppercase()) }
        } ?: return null

        val afterColon = line.substringAfter(":", "").trim()
        if (afterColon.isNotBlank()) return afterColon

        val keyword = keywords.first { kw -> line.uppercase().contains(kw.uppercase()) }
        val startIdx = line.uppercase().indexOf(keyword.uppercase()) + keyword.length
        val afterKeyword = line.substring(startIdx).trim()
        if (afterKeyword.isNotBlank()) return afterKeyword

        return extractLineAfterKeyword(linhas, keywords)
    }

    private fun findLineContaining(linhas: List<String>, keywords: List<String>): String? =
        linhas.firstOrNull { line ->
            keywords.any { kw -> line.uppercase().contains(kw.uppercase()) }
        }

    private fun getNextContentLine(linhas: List<String>, labelLine: String): String?
    {
        val idx = linhas.indexOf(labelLine)
        if (idx < 0) return null
        for (i in (idx + 1) until linhas.size)
        {
            if (!isLinhaLabel(linhas[i])) return linhas[i]
        }
        return null
    }

    private fun isLinhaLabel(linha: String): Boolean
    {
        val up = linha.uppercase()
        val labelKeywords = listOf(
            "NOME", "SOBRENOME", "HABILITAÇÃO", "EMISSÃO", "VALIDADE",
            "REGISTRO", "IDENTIDADE", "CPF", "CATEGORIA", "NATURALIDADE",
            "FILIAÇÃO", "NACIONAL", "DATA", "LOCAL", "NASCIMENTO", "CAT",
            "ADMINISTRAÇÃO", "CONDUÇÃO", "SECRETARIA", "DETRAN",
            // CRLV
            "RENAVAM", "CHASSI", "MARCA", "MODELO", "COMBUSTÍVEL",
            "ESPÉCIE", "PROPRIETÁRIO", "MUNICIPIO", "MUNICÍPIO",
            "EXERCÍCIO", "LICENCIAMENTO", "CRLV"
        )
        val palavras = up.trim().split(Regex("\\s+"))
        return palavras.size <= 5 && labelKeywords.any { up.contains(it) }
    }

    private fun extrairNomeFallBack(linhas: List<String>): String?
    {
        val ignorar = setOf(
            "REPÚBLICA", "FEDERATIVA", "BRASIL", "HABILITAÇÃO", "DETRAN",
            "MINISTÉRIO", "INFRAESTRUTURA", "SECRETARIA", "SENATRAN",
            "DENATRAN", "CARTEIRA", "NACIONAL", "DRIVER", "LICENSE",
            "PERMISO", "CONDUCCIÓN", "BRASILEIRO", "BRASILEIRA"
        )
        return linhas.firstOrNull { linha ->
            val up = linha.uppercase()
            up == linha &&
                    linha.split(" ").size >= 2 &&
                    linha.length > 6 &&
                    linha.none { it.isDigit() } &&
                    ignorar.none { up.contains(it) }
        }
    }

    private fun String.formatarCpf(): String
    {
        val digits = replace(Regex("""\D"""), "")
        return if (digits.length == 11)
            "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${
                digits.substring(6, 9)
            }-${digits.substring(9)}"
        else this
    }
}