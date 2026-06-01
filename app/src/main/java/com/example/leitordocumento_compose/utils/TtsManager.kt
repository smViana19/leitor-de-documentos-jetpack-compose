package com.example.leitordocumento_compose.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(context: Context)
{
    private var tts: TextToSpeech? = null
    private var ultimaMensagemFalada = ""
    private var inicializado = false

    init
    {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS)
            {
                val resultado = tts?.setLanguage(Locale("pt", "BR"))
                inicializado = resultado != TextToSpeech.LANG_MISSING_DATA && resultado != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun falar(mensagem: String, forceFalar: Boolean = true)
    {
        if (!inicializado) return
        if (mensagem.isEmpty()) return
        if (mensagem == ultimaMensagemFalada && !forceFalar) return

        ultimaMensagemFalada = mensagem
        tts?.speak(mensagem, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun resetarUltimaMensagem() {
        ultimaMensagemFalada = ""
    }
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

}