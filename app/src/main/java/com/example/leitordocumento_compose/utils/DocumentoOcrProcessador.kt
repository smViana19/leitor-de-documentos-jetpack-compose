package com.example.leitordocumento_compose.utils

import com.example.documentscan.DocumentType
import com.example.leitordocumento_compose.data.DadosCNH
import com.example.leitordocumento_compose.data.DadosCRLV
import com.example.leitordocumento_compose.data.DadosRG
import com.example.leitordocumento_compose.data.OcrResultado
import com.google.mlkit.vision.text.Text

/**
 * DocumentoOcrProcessador — abordagem "padrão primeiro, rótulo depois".
 *
 * Filosofia central:
 *  - Campos com formato fixo (placa, chassi, CPF, RENAVAM) são extraídos
 *    por regex direta no texto completo — independente de layout.
 *  - Campos contextuais (nome, município, marca) usam janela de contexto
 *    ao redor de palavras-âncora conhecidas.
 *  - Sem parse linha-por-linha: o CRLV digital mistura rótulos e valores
 *    na mesma linha, tornando essa abordagem não-confiável.
 */
object DocumentoOcrProcessador {

    // ─────────────────────────────────────────────────────────────────────────
    // Regex compartilhadas
    // ─────────────────────────────────────────────────────────────────────────

    // Data DD/MM/AAAA com qualquer separador
    private val RX_DATA = Regex("""(\d{2}[/.\-]\d{2}[/.\-]\d{4})""")

    // CPF formatado: 000.000.000-00 (com separadores flexíveis)
    private val RX_CPF = Regex("""(\d{3}[.\- ]\d{3}[.\- ]\d{3}[.\- ]\d{2})""")

    // CPF compacto: 11 dígitos isolados
    private val RX_CPF_COMPACTO = Regex("""(?<!\d)(\d{11})(?!\d)""")

    // Placa Mercosul (ABC1D23) e antiga (ABC1234)
    private val RX_PLACA = Regex("""(?<![A-Z0-9])([A-Z]{3}\d[A-Z0-9]\d{2})(?![A-Z0-9])""",
        RegexOption.IGNORE_CASE)

    // VIN / Chassi: exatamente 17 chars alfanuméricos sem I, O, Q
    private val RX_CHASSI = Regex("""(?<![A-Z0-9])([A-HJ-NPR-Z0-9]{17})(?![A-Z0-9])""",
        RegexOption.IGNORE_CASE)

    // RENAVAM após palavra-âncora
    private val RX_RENAVAM_ANCORA = Regex(
        """(?:RENAVAM|RENAVAN)\s+(\d{9,11})""", RegexOption.IGNORE_CASE)

    // Ano isolado 1950-2099
    private val RX_ANO = Regex("""(?<!\d)((?:19[5-9]\d|20\d{2}))(?!\d)""")

    // Validade MM/AAAA
    private val RX_VALIDADE_CRLV = Regex("""(\d{2}/\d{4})""")

    // Potência e cilindrada: 85CV/997
    private val RX_POTENCIA = Regex("""(\d+)\s*CV[/\s](\d+)""", RegexOption.IGNORE_CASE)

    // Número do CRV (para excluir da busca de RENAVAM)
    private val RX_CRV = Regex("""(?:NUMERO\s+DO\s+CRV|N.MERO\s+DO\s+CRV)\s+(\d+)""",
        RegexOption.IGNORE_CASE)

    private val CORES = setOf(
        "AMARELA","AZUL","BEGE","BRANCA","CINZA","DOURADA","GRENA","LARANJA",
        "MARROM","PRATA","PRETA","ROSA","ROXA","VERDE","VERMELHA","FANTASIA"
    )

    private val MARCAS = listOf(
        "FORD","VOLKSWAGEN","VW","FIAT","CHEVROLET","GM","TOYOTA","HONDA",
        "HYUNDAI","RENAULT","NISSAN","BMW","MERCEDES","JEEP","DODGE",
        "MITSUBISHI","CHERY","JAC","CAOA","YAMAHA","KAWASAKI","SUZUKI",
        "VOLVO","SCANIA","IVECO","PEUGEOT","CITROEN","LAND ROVER","MINI"
    )

    // Categorias válidas de CNH
    private val CATEGORIAS_CNH = setOf("A","B","C","D","E","AB","AC","AD","AE")

    // Categorias válidas de veículo no CRLV
    private val CATEGORIAS_VEICULO = setOf(
        "PARTICULAR","ALUGUEL","OFICIAL","APRENDIZAGEM","COLEÇÃO",
        "DIPLOMATICO","EXPERIENCIA","FABRICANTE"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Ponto de entrada
    // ─────────────────────────────────────────────────────────────────────────

    fun processarDocumento(mlKitTexto: Text, tipoDocumentoSelecionado: DocumentType): OcrResultado {
        val raw = mlKitTexto.text
        val up = raw.uppercase()
        return when {
            isCrlv(up) -> OcrResultado.Crlv(processarCrlv(raw, up))
            isCnh(up)  -> OcrResultado.Cnh(processarCnh(raw, up, mlKitTexto))
            isRg(up)   -> OcrResultado.Rg(processarRg(raw, up))
            else       -> OcrResultado.Desconhecido(rawText = raw)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detecção de tipo
    // ─────────────────────────────────────────────────────────────────────────

    private fun isCrlv(up: String): Boolean {
        var score = 0
        if (up.contains("RENAVAM"))                            score += 5
        if (up.contains("LICENCIAMENTO"))                      score += 4
        if (up.contains("CERTIFICADO DE REGISTRO"))            score += 5
        if (up.contains("CRLV"))                               score += 5
        if (up.contains("CHASSI") || up.contains("CHASS"))    score += 3
        if (up.contains("MARCA") && up.contains("MODELO"))     score += 2
        if (up.contains("EXERC"))                              score += 2
        if (up.contains("HABILITAÇÃO"))                        score -= 8
        if (up.contains("IDENTIDADE"))                         score -= 8
        return score >= 5
    }

    private fun isCnh(up: String): Boolean =
        up.contains("HABILITAÇÃO") ||
                up.contains("CARTEIRA NACIONAL") ||
                up.contains("DRIVER LICENSE") ||
                up.contains("DETRAN") ||
                up.contains("SENATRAN") ||
                up.contains("DENATRAN") ||
                up.contains("MINISTÉRIO DA INFRAESTRUTURA") ||
                up.contains("SECRETARIA NACIONAL DE TRÂNSITO")

    private fun isRg(up: String): Boolean =
        up.contains("IDENTIDADE") ||
                up.contains("REGISTRO GERAL") ||
                up.contains("REPÚBLICA FEDERATIVA") ||
                (up.contains("SECRETARIA") && up.contains("SEGURANÇA"))

    // ─────────────────────────────────────────────────────────────────────────
    // CRLV
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extrai todos os campos do CRLV por regex direta no texto completo.
     * Funciona tanto para o CRLV digital (layout caótico) quanto para o físico.
     */
    private fun processarCrlv(raw: String, up: String): DadosCRLV {

        // Texto normalizado para campos numéricos (O→0, I→1 etc.) sem quebrar nomes
        val norm = normalizarNumerico(up)

        val placa = extrairPlacaCrlv(up)
        val renavam = extrairRenavam(up)
        val chassi = extrairChassi(norm)
        val cpfCnpj = extrairCpf(raw) ?: extrairCnpj(raw)
        val proprietario = extrairProprietario(up)
        val (marca, modelo) = extrairMarcaModelo(up)
        val cor = extrairCor(up)
        val (anoFab, anoMod) = extrairAnosFabMod(up, renavam)
        val exercicio = extrairExercicio(up)
        val (municipio, uf) = extrairMunicipioUf(up)
        val validade = RX_VALIDADE_CRLV.find(up)?.value
        val (potencia, cilindrada) = extrairPotenciaCilindrada(up)
        val especie = extrairPalavraApos(up, listOf("ESPECIE","ESPÉCIE","ESPTCE","ESPCEI","ESPE[CÇ]IE"))
        val combustivel = extrairCombustivel(up)
        val categoria = CATEGORIAS_VEICULO.firstOrNull { up.contains(it) }

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
            tipo = null,
            categoria = categoria,
            municipio = municipio,
            uf = uf,
            validade = validade,
            exercicio = exercicio,
            potencia = potencia,
            cilindrada = cilindrada,
            pbt = null,
            cmt = null,
            capacidade = null,
            rawText = raw
        )
    }

    // Placa — primeira ocorrência não precedida de contexto motor
    private fun extrairPlacaCrlv(up: String): String? {
        // Evita capturar o número do motor (padrão similar)
        val excluirApos = setOf("MOTOR", "CRV", "ANTERIOR")
        return RX_PLACA.findAll(up).firstOrNull { m ->
            val antes = up.substring(maxOf(0, m.range.first - 15), m.range.first).trim()
            excluirApos.none { antes.endsWith(it) }
        }?.value?.uppercase()?.replace(" ","")?.replace("-","")?.let { bruto ->
            if (bruto.length == 7 && bruto[3].isDigit() && bruto[4].isLetter()) bruto
            else if (bruto.length == 7) "${bruto.take(3)}-${bruto.drop(3)}"
            else bruto
        }
    }

    // RENAVAM — âncora textual "RENAVAM/RENAVAN" + dígitos
    private fun extrairRenavam(up: String): String? {
        val porAncora = RX_RENAVAM_ANCORA.find(up)?.groupValues?.get(1)
        if (porAncora != null) return porAncora

        // Fallback: número de 11 dígitos que não seja o CRV
        val crv = RX_CRV.find(up)?.groupValues?.get(1) ?: ""
        return Regex("""(?<!\d)(\d{11})(?!\d)""").findAll(up)
            .map { it.groupValues[1] }
            .firstOrNull { it != crv && !lembaCpf(it, up) }
    }

    // Chassi VIN 17 chars — texto normalizado para corrigir I/O/Q do OCR
    private fun extrairChassi(norm: String): String? {
        // Tenta com texto sem espaços (OCR pode fragmentar o VIN)
        val semEspacos = norm.replace(" ", "")
        val m1 = RX_CHASSI.find(semEspacos)?.value
        if (m1 != null) return m1

        // Tenta no texto com espaços
        return RX_CHASSI.find(norm)?.value
    }

    // CPF com pontuação: 000.000.000-00
    private fun extrairCpf(raw: String): String? {
        return RX_CPF.find(raw)?.value
    }

    // CNPJ: 00.000.000/0000-00
    private fun extrairCnpj(raw: String): String? =
        Regex("""\d{2}\.?\d{3}\.?\d{3}/?\d{4}-?\d{2}""").find(raw)?.value

    // Proprietário — aparece após MUNICÍPIO+UF, antes de DADOS/SEGURO/CPF
    private fun extrairProprietario(up: String): String? {
        // Padrão: "... UF [NOME EM MAIÚSCULAS] DADOS/SEGURO/CPF"
        val m = Regex("""\b([A-Z]{2})\s+([A-Z]{3,}(?:\s+[A-Z]{2,}){2,6}?)\s+(?:DADOS|CPF|REPASSE|SEGURO)""")
            .find(up)
        if (m != null) {
            val candidato = m.groupValues[2].trim()
            if (candidato.split(" ").size >= 2) return candidato
        }

        // Fallback: NOME LOCAL ... UF NOME_PROPRIO
        val m2 = Regex("""(?:NOME\s+LOCAL|LOCAL)\s+\S+(?:\s+\S+)?\s+[A-Z]{2}\s+([A-Z]{3,}(?:\s+[A-Z]{3,}){1,5})""")
            .find(up)
        return m2?.groupValues?.get(1)?.trim()
            ?.takeIf { it.split(" ").size >= 2 }
    }

    // Marca e modelo: FORD/KA SE 1.0 (barra como separador ou espaço)
    private fun extrairMarcaModelo(up: String): Pair<String?, String?> {
        for (marca in MARCAS) {
            // Padrão: MARCA/MODELO ou MARCA MODELO (sem outra palavra maiúscula antes)
            val rx = Regex("""(?<![A-Z])(${Regex.escape(marca)})[/ ]([A-Z0-9][A-Z0-9/\.\s]{2,30}?)(?=\s{2,}|\s+(?:${CORES.joinToString("|")})|ESP|COR|PRDOMINANTE|COMBUSTIVEL|ALCOOL|GASOLINA|PASSAGEIRO|AUTOMOVEL|CATEGORIA|POTENCIA)""")
            val m = rx.find(up)
            if (m != null) {
                return Pair(m.groupValues[1].trim(), m.groupValues[2].trim())
            }
        }

        // Fallback: qualquer PALAVRA/PALAVRA2 com barra (VW/GOL etc.)
        val mBarra = Regex("""([A-Z]{2,})/([A-Z0-9][A-Z0-9\s\.]{2,20}?)(?=\s+(?:${CORES.joinToString("|")})|ESP|COR|\s{2})""")
            .find(up)
        if (mBarra != null) {
            return Pair(mBarra.groupValues[1].trim(), mBarra.groupValues[2].trim())
        }

        return Pair(null, null)
    }

    // Cor — busca a primeira cor oficial no texto
    private fun extrairCor(up: String): String? =
        CORES.firstOrNull { up.contains(it) }

    // Anos de fabricação e modelo — exclui o exercício e o CRV
    private fun extrairAnosFabMod(up: String, renavam: String?): Pair<String?, String?> {
        val excluir = mutableSetOf<String>()

        // Exercício
        extrairExercicio(up)?.let { excluir.add(it) }
        // CRV — primeiros 4 dígitos se for longo
        RX_CRV.find(up)?.groupValues?.get(1)?.take(4)?.let { excluir.add(it) }

        // "ANO FABRICACAO XXXX" e "ANO MODELO XXXX"
        val fabExplicito = Regex("""(?:ANO\s+FABRICA[CÇ][AÃ]O|ANO\s+FAB\.?)\s+(\d{4})""", RegexOption.IGNORE_CASE)
            .find(up)?.groupValues?.get(1)
        val modExplicito = Regex("""(?:ANO\s+MODELO?|ANO\s+MOD\.?)\s+(\d{4})""", RegexOption.IGNORE_CASE)
            .find(up)?.groupValues?.get(1)

        if (fabExplicito != null && modExplicito != null) return Pair(fabExplicito, modExplicito)

        // Fallback: primeiros dois anos que não estão na lista de exclusão
        val todos = RX_ANO.findAll(up).map { it.groupValues[1] }
            .filter { it !in excluir }
            .distinct().toList()

        return Pair(todos.getOrNull(0) ?: fabExplicito, todos.getOrNull(1) ?: modExplicito)
    }

    // Exercício: ano de licenciamento — normalmente o mais recente
    private fun extrairExercicio(up: String): String? {
        val m = Regex("""(?:EXERCI[CÇ][OA0]|EXERCIC[OA])\s+(?:\S+\s+){0,3}?(\d{4})""",
            RegexOption.IGNORE_CASE).find(up)
        return m?.groupValues?.get(1)
    }

    // Município e UF — "CIDADE UF" onde UF = 2 letras maiúsculas
    private fun extrairMunicipioUf(up: String): Pair<String?, String?> {
        // Padrão: NOME LOCAL CIDADE UF / LOCAL CIDADE-UF
        val m = Regex("""(?:NOME\s+LOCAL|LOCAL)\s+([A-ZÁÉÍÓÚÂÊÔÇ][A-ZÁÉÍÓÚÂÊÔÇ\s]+?)\s+\b([A-Z]{2})\b""")
            .find(up)
        if (m != null) return Pair(m.groupValues[1].trim(), m.groupValues[2])

        // Fallback: qualquer CIDADE-UF (2 palavras maiúsculas + sigla)
        val m2 = Regex("""([A-Z][A-Z\s]{3,20}?)\s+\b(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)\b""")
            .find(up)
        return Pair(m2?.groupValues?.get(1)?.trim(), m2?.groupValues?.get(2))
    }

    // Potência e cilindrada: "85CV/997" ou "85 CV 997"
    private fun extrairPotenciaCilindrada(up: String): Pair<String?, String?> {
        val m = RX_POTENCIA.find(up) ?: return Pair(null, null)
        return Pair(m.groupValues[1], m.groupValues[2])
    }

    // Combustível: ALCOOL/GASOLINA, FLEX, DIESEL, ELETRICO
    private fun extrairCombustivel(up: String): String? {
        val padroes = listOf(
            Regex("""(ALCOOL[\s/]+GASOLINA|FLEX)""", RegexOption.IGNORE_CASE),
            Regex("""(GASOLINA)""", RegexOption.IGNORE_CASE),
            Regex("""(DIESEL)""", RegexOption.IGNORE_CASE),
            Regex("""(ELETRICO|ELÉTRICO)""", RegexOption.IGNORE_CASE),
            Regex("""(GNV)""", RegexOption.IGNORE_CASE)
        )
        return padroes.firstNotNullOfOrNull { it.find(up)?.groupValues?.get(1)?.uppercase() }
    }

    // Palavra após um rótulo (espécie, tipo etc.)
    private fun extrairPalavraApos(up: String, rotulos: List<String>): String? {
        for (rotulo in rotulos) {
            val m = Regex("""$rotulo\s+(\S+)""", RegexOption.IGNORE_CASE).find(up)
            if (m != null) return m.groupValues[1].uppercase()
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CNH
    // ─────────────────────────────────────────────────────────────────────────

    private fun processarCnh(raw: String, up: String, mlText: Text): DadosCNH {
        val datas = RX_DATA.findAll(raw).map { it.value }.toList()
        val cpf = extrairCpf(raw) ?: extrairCpfCompacto(raw, up)
        val rg = extrairRg(raw, up, cpf)
        val nome = extrairNomeCnh(up, mlText)
        val categoria = extrairCategoriaCnh(up)
        val numeroRegistro = extrairNumeroRegistroCnh(up, cpf)
        val filiacao = extrairJanela(up, listOf("FILIAÇÃO","FILIACAO"), stopWords = PALAVRAS_CHAVE_CNH)
        val localNascimento = extrairJanela(up, listOf("LOCAL","NASCIMENTO"), stopWords = PALAVRAS_CHAVE_CNH)
        val nacionalidade = extrairJanela(up, listOf("NACIONALIDADE"), stopWords = PALAVRAS_CHAVE_CNH)

        // Atribui datas por posição contextual
        val dataPool = datas.toMutableList()
        fun proxData() = dataPool.removeFirstOrNull()

        val dataNasc = extrairDataApos(up, listOf("NASC","DATA DE NASC")) ?: proxData()
        val primeiraHab = extrairDataApos(up, listOf("1ª HAB","1A HAB","PRIMEIRA HAB")) ?: proxData()
        val emissao = extrairDataApos(up, listOf("EMISSÃO","EMISSAO","DATA EMISSÃO")) ?: proxData()
        val validade = extrairDataApos(up, listOf("VALIDADE","VALID")) ?: proxData()

        return DadosCNH(
            nome = nome,
            cpf = cpf,
            rg = rg,
            dataNascimento = dataNasc,
            localNascimento = localNascimento,
            numeroRegistro = numeroRegistro,
            primeiraHabilitacao = primeiraHab,
            dataEmissao = emissao,
            dataValidade = validade,
            categoria = categoria,
            orgaoEmissor = null,
            filiacao = filiacao,
            nacionalidade = nacionalidade,
            rawText = raw
        )
    }

    /**
     * Nome na CNH: busca em cascata.
     * 1. Rótulo "NOME" ou "2E1" → próxima linha/bloco não-rótulo
     * 2. Blocos do ML Kit: bloco após bloco contendo rótulo
     * 3. Heurística: linha toda maiúsculas, ≥2 palavras, ≥8 chars, sem dígitos
     */
    private fun extrairNomeCnh(up: String, mlText: Text): String? {
        val rotulosNome = listOf("NOME E SOBRENOME","NOME SOBRENOME","NOME COMPLETO","NOME","2E1","2 E 1")
        val ignorar = setOf("REPÚBLICA","FEDERATIVA","BRASIL","HABILITAÇÃO","DETRAN","MINISTÉRIO",
            "SECRETARIA","SENATRAN","DENATRAN","CARTEIRA","NACIONAL","DRIVER","BRASILEIRO",
            "REGISTRO","CATEGORIA","VALIDADE","EMISSÃO","FILIAÇÃO","IDENTIDADE","NATURALIDADE")

        // 1. Rótulo no texto plano
        for (rotulo in rotulosNome) {
            val idx = up.indexOf(rotulo)
            if (idx < 0) continue
            val depois = up.substring(idx + rotulo.length).trimStart()
            // Pega a sequência de letras e espaços até o próximo separador
            val candidato = Regex("""^([A-ZÁÉÍÓÚÂÊÔÇ]{3,}(?:\s+[A-ZÁÉÍÓÚÂÊÔÇ]{2,}){1,6})""")
                .find(depois)?.groupValues?.get(1)?.trim()
            if (candidato != null && ignorar.none { candidato.contains(it) }) return candidato
        }

        // 2. Blocos do ML Kit
        for ((i, block) in mlText.textBlocks.withIndex()) {
            if (rotulosNome.any { block.text.uppercase().contains(it) }) {
                val prox = mlText.textBlocks.getOrNull(i + 1)?.text?.trim() ?: continue
                if (pareceNome(prox) && ignorar.none { prox.uppercase().contains(it) })
                    return prox
            }
        }

        // 3. Heurística
        return up.lines()
            .map { it.trim() }
            .firstOrNull { linha ->
                linha.length >= 8 &&
                        linha.split(" ").size >= 2 &&
                        linha.none { it.isDigit() } &&
                        ignorar.none { linha.contains(it) } &&
                        linha.all { it.isLetter() || it == ' ' || it == '\'' || it == '-' }
            }
    }

    private fun extrairRg(raw: String, up: String, cpf: String?): String? {
        val cpfDigits = cpf?.replace(Regex("""\D"""), "") ?: ""
        // RG formatado: XX.XXX.XXX-X
        val mFormatado = Regex("""\d{1,2}\.\d{3}\.\d{3}-[\dXx]""").find(raw)
        if (mFormatado != null) return mFormatado.value

        // Contexto: após "RG" ou "IDENTIDADE"
        val mContexto = Regex("""(?:RG|IDENTIDADE|DOC\.?\s+IDENTIDADE)\s+([\d.\-X]+)""",
            RegexOption.IGNORE_CASE).find(up)
        val candidato = mContexto?.groupValues?.get(1)
        if (candidato != null && candidato.replace(Regex("""\D"""),"") != cpfDigits)
            return candidato

        return null
    }

    private fun extrairCategoriaCnh(up: String): String? {
        // Contexto direto
        val m = Regex("""(?:CAT\.?\s*HAB\.?|CATEGORIA\s+HAB|CATEGORIA)\s+([ABCDE]{1,2})(?![A-Z])""",
            RegexOption.IGNORE_CASE).find(up)
        if (m != null && m.groupValues[1].uppercase() in CATEGORIAS_CNH)
            return m.groupValues[1].uppercase()

        // Linha isolada com apenas a categoria
        return up.lines().map { it.trim() }
            .firstOrNull { it.uppercase() in CATEGORIAS_CNH }
    }

    private fun extrairNumeroRegistroCnh(up: String, cpf: String?): String? {
        val cpfDigits = cpf?.replace(Regex("""\D"""), "") ?: ""
        val m = Regex("""(?:REGISTRO|Nº\s*REGISTRO|N°\s*REGISTRO)\s+(\d{9,11})""",
            RegexOption.IGNORE_CASE).find(up)
        return m?.groupValues?.get(1)?.takeIf { it != cpfDigits }
            ?: Regex("""(?<!\d)(\d{11})(?!\d)""").findAll(up)
                .map { it.groupValues[1] }
                .firstOrNull { it != cpfDigits }
    }

    private fun extrairDataApos(up: String, rotulos: List<String>): String? {
        for (rotulo in rotulos) {
            val idx = up.indexOf(rotulo)
            if (idx < 0) continue
            val m = RX_DATA.find(up.substring(idx))
            if (m != null) return m.value
        }
        return null
    }

    /**
     * Extrai o valor de texto após um rótulo, parando na primeira stopword.
     */
    private fun extrairJanela(up: String, rotulos: List<String>, stopWords: Set<String>): String? {
        for (rotulo in rotulos) {
            val idx = up.indexOf(rotulo)
            if (idx < 0) continue
            val trecho = up.substring(idx + rotulo.length).trimStart()
            // Pega até 40 chars ou até encontrar dígito/stopword
            val candidato = Regex("""^([A-ZÁÉÍÓÚÂÊÔÇ\s\-']{4,40}?)(?=\s+\d|\s{3,}|$)""")
                .find(trecho)?.groupValues?.get(1)?.trim()
            if (!candidato.isNullOrBlank() && stopWords.none { candidato.contains(it) })
                return candidato
        }
        return null
    }

    private val PALAVRAS_CHAVE_CNH = setOf(
        "HABILITAÇÃO","EMISSÃO","VALIDADE","REGISTRO","CATEGORIA","CPF","FILIAÇÃO",
        "NACIONAL","DETRAN","SENATRAN","DATA","LOCAL"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // RG
    // ─────────────────────────────────────────────────────────────────────────

    private fun processarRg(raw: String, up: String): DadosRG {
        val datas = RX_DATA.findAll(raw).map { it.value }.toList()
        val cpf = extrairCpf(raw)
        val rg = extrairRg(raw, up, cpf)
        val nome = extrairNomeRg(up)
        val nomeMae = extrairJanela(up, listOf("MÃE","MAE","FILIAÇÃO","FILIACAO"),
            stopWords = setOf("PAI","DATA","NATURAL","CPF","RG"))
        val nomePai = extrairJanela(up, listOf("PAI"),
            stopWords = setOf("MÃE","DATA","NATURAL","CPF","RG"))
        val naturalidade = extrairJanela(up, listOf("NATURALIDADE","NATURAL"),
            stopWords = setOf("DATA","CPF","RG","MÃE"))

        return DadosRG(
            nome = nome,
            rg = rg,
            cpf = cpf,
            dataNascimento = datas.firstOrNull(),
            nomeMae = nomeMae,
            nomePai = nomePai,
            naturalidade = naturalidade,
            dataEmissao = datas.lastOrNull(),
            rawText = raw
        )
    }

    private fun extrairNomeRg(up: String): String? {
        val porRotulo = Regex("""(?:NOME)\s+([A-ZÁÉÍÓÚÂÊÔÇ]{3,}(?:\s+[A-ZÁÉÍÓÚÂÊÔÇ]{2,}){1,6})""")
            .find(up)?.groupValues?.get(1)?.trim()
        if (porRotulo != null) return porRotulo

        val ignorar = setOf("REPÚBLICA","FEDERATIVA","BRASIL","SECRETARIA","SEGURANÇA","IDENTIDADE")
        return up.lines().map { it.trim() }.firstOrNull { linha ->
            pareceNome(linha) && ignorar.none { linha.contains(it) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitários
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Normaliza apenas a parte numérica/alfanumérica para extrair chassi e RENAVAM.
     * Substitui letras ambíguas por dígitos — NÃO usar em campos de texto livre.
     */
    private fun normalizarNumerico(texto: String): String =
        texto.uppercase()
            .replace('O', '0')
            .replace('I', '1')
            .replace('Q', '0')
            .replace('S', '5')

    /** Verifica se uma string parece nome próprio (≥2 palavras, sem dígitos) */
    private fun pareceNome(texto: String): Boolean {
        val limpo = texto.trim()
        return limpo.length >= 6 &&
                limpo.split(" ").size >= 2 &&
                limpo.none { it.isDigit() } &&
                limpo.all { it.isLetter() || it == ' ' || it == '\'' || it == '-' }
    }

    /** Verifica se um número de 11 dígitos aparece no texto com formatação de CPF */
    private fun lembaCpf(num: String, up: String): Boolean {
        val cpfFormatado = Regex("""\d{3}\.\d{3}\.\d{3}-\d{2}""")
        return cpfFormatado.findAll(up).any { m ->
            m.value.replace(Regex("""\D"""), "") == num
        }
    }

    /** CPF compacto: 11 dígitos isolados (sem formatação) — usado como último recurso */
    private fun extrairCpfCompacto(raw: String, up: String): String? {
        return RX_CPF_COMPACTO.findAll(raw)
            .map { it.groupValues[1] }
            .firstOrNull { num ->
                // Não é CRV, não é RENAVAM já confirmado, não é ano
                num.length == 11 && !RX_ANO.containsMatchIn(num)
            }?.let { digits ->
                "${digits.substring(0,3)}.${digits.substring(3,6)}.${digits.substring(6,9)}-${digits.substring(9)}"
            }
    }
}