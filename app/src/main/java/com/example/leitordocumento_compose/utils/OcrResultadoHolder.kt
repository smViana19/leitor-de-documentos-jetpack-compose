package com.example.leitordocumento_compose.utils

import androidx.compose.animation.AnimatedContentScope
import androidx.navigation.NavBackStackEntry
import com.example.leitordocumento_compose.data.OcrResultado


object OcrResultadoHolder {
    var resultado: OcrResultado? = null

    fun set(r: OcrResultado) { resultado = r }
    fun consume(): OcrResultado? = resultado.also { resultado = null }
}
