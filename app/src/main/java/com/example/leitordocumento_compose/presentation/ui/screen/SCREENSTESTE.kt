package com.example.leitordocumento_compose.presentation.ui.screen

import ResultadoPlaca

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.leitordocumento_compose.R
import com.example.leitordocumento_compose.data.DadosCNH
import com.example.leitordocumento_compose.data.DadosRG

// ── Paleta ────────────────────────────────────────────────────────────────────
private val BgCard     = Color(0xFF111827)
private val BgField    = Color(0xFF151E2E)
private val AccentBlue = Color(0xFF4A90D9)
private val TextPrim   = Color(0xFFFFFFFF)
val TextSec    = Color(0xFF8A9BB5)
private val DivCol     = Color(0xFF1E2D45)
private val GreenOk    = Color(0xFF1D9E75)
val Amber      = Color(0xFFE8A020)

// ═════════════════════════════════════════════════════════════════════════════
// 1. CNH
// ═════════════════════════════════════════════════════════════════════════════



// ═════════════════════════════════════════════════════════════════════════════
// 2. RG
// ═════════════════════════════════════════════════════════════════════════════



// ═════════════════════════════════════════════════════════════════════════════
// 3. Placa
// ═════════════════════════════════════════════════════════════════════════════



// ═════════════════════════════════════════════════════════════════════════════
// 4. CRLV  (estrutura pronta para quando o OCR for implementado)
// ═════════════════════════════════════════════════════════════════════════════

// ═════════════════════════════════════════════════════════════════════════════
// Scaffold compartilhado entre todos os formulários
// ═════════════════════════════════════════════════════════════════════════════



// ═════════════════════════════════════════════════════════════════════════════
// Componentes base
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SecaoCard(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            titulo.uppercase(), color = TextSec,
            fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 0.5.dp)
        content()
    }
}



// ═════════════════════════════════════════════════════════════════════════════
// Helpers
// ═════════════════════════════════════════════════════════════════════════════


private fun String.blankToNull(): String? = ifBlank { null }