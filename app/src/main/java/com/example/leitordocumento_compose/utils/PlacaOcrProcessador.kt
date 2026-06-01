import android.util.Log

data class ResultadoPlaca(
    val placa: String,
//    val tipo: TipoVeiculo,
//    val padrao: PadraoPlaca,
    val placaNormalizada: String
)

enum class TipoVeiculo {
    CARRO, MOTO
}

enum class PadraoPlaca {
    MERCOSUL, BRASIL_ANTIGO
}

object PlacaOcrProcessador {

    /**
     * Mercosul CARRO: 3 letras + 1 número + 1 letra + 2 números
     * Ex: ABC1D23
     */
    private val placaMercosulCarroRegex = Regex("""[A-Z]{3}[0-9][A-Z][0-9]{2}""")

    /**
     * Mercosul MOTO: igual ao carro, mas a letra do meio costuma ser
     * identificada pelo contexto (mesmo regex, diferenciamos por tamanho/contexto).
     * Na prática, o padrão é idêntico — a distinção moto/carro vem de dados extras.
     */
    private val placaMercosulMotoRegex = Regex("""[A-Z]{3}[0-9][A-Z][0-9]{2}""")

    /**
     * Brasil Antigo CARRO e MOTO: 3 letras + 4 números
     * Ex: ABC1234
     * Motos antigas têm o mesmo formato — distinção feita por comprimento/contexto.
     */
    private val placaBrasilRegex = Regex("""[A-Z]{3}[0-9]{4}""")

    /**
     * Processa o texto bruto retornado pelo OCR e tenta extrair uma placa válida.
     *
     * @param textoOcr  Texto cru reconhecido pela câmera/OCR
     * @param tipoVeiculoHint  Hint opcional: se o app já souber o tipo (carro/moto),
     *                         passa aqui para desempate. Null = auto-detectar.
     * @return ResultadoPlaca se encontrado, null caso contrário.
     */
    fun processarPlaca(
        textoOcr: String,
        tipoVeiculoHint: TipoVeiculo? = null
    ): ResultadoPlaca? {
        val textoNormalizado = normalizarTexto(textoOcr)
        Log.d("PlacaOCR", "Texto normalizado: $textoNormalizado")

        // Tenta Mercosul primeiro (padrão mais recente e mais restritivo)
        val matchMercosul = placaMercosulCarroRegex.find(textoNormalizado)
        if (matchMercosul != null) {
            val placa = matchMercosul.value
            val tipo = tipoVeiculoHint ?: resolverTipoMercosul(placa)
            return ResultadoPlaca(
                placa = placa,
//                tipo = tipo,
//                padrao = PadraoPlaca.MERCOSUL,
                placaNormalizada = formatarPlaca(placa, PadraoPlaca.MERCOSUL)
            )
        }

        // Tenta padrão antigo
        val matchAntigo = placaBrasilRegex.find(textoNormalizado)
        if (matchAntigo != null) {
            val placa = matchAntigo.value
            val tipo = tipoVeiculoHint ?: TipoVeiculo.CARRO // antigo não tem distinção clara
            return ResultadoPlaca(
                placa = placa,
//                tipo = tipo,
//                padrao = PadraoPlaca.BRASIL_ANTIGO,
                placaNormalizada = formatarPlaca(placa, PadraoPlaca.BRASIL_ANTIGO)
            )
        }

        Log.w("PlacaOCR", "Nenhuma placa válida encontrada em: $textoNormalizado")
        return null
    }



    /**
     * Normaliza o texto do OCR: remove espaços, converte para maiúsculas,
     * corrige erros comuns de OCR (0↔O, 1↔I, 8↔B etc.) nas posições certas.
     */
    private fun normalizarTexto(texto: String): String {
        return texto
            .uppercase()
            .replace(Regex("""[^A-Z0-9]"""), "") // remove tudo que não é letra/número
            .let { corrigirErrosOcr(it) }
    }

    /**
     * Corrige erros clássicos de OCR de acordo com a posição esperada na placa.
     *
     * Padrão Mercosul:  P0 P1 P2 = letras | P3 = número | P4 = letra | P5 P6 = números
     * Padrão antigo:    P0 P1 P2 = letras | P3 P4 P5 P6 = números
     *
     * Aplica a correção sobre uma "janela deslizante" de 7 caracteres.
     */
    private fun corrigirErrosOcr(texto: String): String {
        if (texto.length < 7) return texto

        val sb = StringBuilder(texto)

        for (inicio in 0..(texto.length - 7)) {
            val janela = texto.substring(inicio, inicio + 7)

            // Detecta se parece Mercosul (pos 4 deveria ser letra)
            val pareceMercosul = janela[3].isDigit() && janela[4].isLetter()

            if (pareceMercosul) {
                // Posições 0,1,2,4 → forçar letra; posições 3,5,6 → forçar número
                sb[inicio + 0] = forcarLetra(janela[0])
                sb[inicio + 1] = forcarLetra(janela[1])
                sb[inicio + 2] = forcarLetra(janela[2])
                sb[inicio + 3] = forcarNumero(janela[3])
                sb[inicio + 4] = forcarLetra(janela[4])
                sb[inicio + 5] = forcarNumero(janela[5])
                sb[inicio + 6] = forcarNumero(janela[6])
            } else {
                // Assume padrão antigo: pos 0,1,2 → letra; pos 3,4,5,6 → número
                sb[inicio + 0] = forcarLetra(janela[0])
                sb[inicio + 1] = forcarLetra(janela[1])
                sb[inicio + 2] = forcarLetra(janela[2])
                sb[inicio + 3] = forcarNumero(janela[3])
                sb[inicio + 4] = forcarNumero(janela[4])
                sb[inicio + 5] = forcarNumero(janela[5])
                sb[inicio + 6] = forcarNumero(janela[6])
            }
        }

        return sb.toString()
    }

    /** Converte caracteres frequentemente confundidos com letras pelo OCR */
    private fun forcarLetra(c: Char): Char = when (c) {
        '0' -> 'O'
        '1' -> 'I'
        '8' -> 'B'
        '6' -> 'G'
        '5' -> 'S'
        else -> c
    }

    /** Converte caracteres frequentemente confundidos com números pelo OCR */
    private fun forcarNumero(c: Char): Char = when (c) {
        'O', 'Q' -> '0'
        'I', 'L' -> '1'
        'B'      -> '8'
        'G'      -> '6'
        'S', 'Z' -> '5' // Z↔2 também é comum; ajuste conforme seu dataset
        'Z'      -> '2'
        else -> c
    }

    /**
     * No Mercosul não há como distinguir carro/moto só pela placa —
     * o DENATRAN usa a mesma máscara. Retorna CARRO como padrão;
     * sobrescreva com o hint quando disponível.
     */
    private fun resolverTipoMercosul(placa: String): TipoVeiculo {
        // Futuramente: consulta API DENATRAN/SERPRO para obter categoria real
        return TipoVeiculo.CARRO
    }

    /**
     * Formata a placa com hífen para exibição.
     * Mercosul:  ABC1D23  → ABC1D23  (sem hífen, padrão atual)
     * Antigo:    ABC1234  → ABC-1234
     */
    private fun formatarPlaca(placa: String, padrao: PadraoPlaca): String {
        return when (padrao) {
            PadraoPlaca.MERCOSUL     -> placa
            PadraoPlaca.BRASIL_ANTIGO -> "${placa.take(3)}-${placa.drop(3)}"
        }
    }
}