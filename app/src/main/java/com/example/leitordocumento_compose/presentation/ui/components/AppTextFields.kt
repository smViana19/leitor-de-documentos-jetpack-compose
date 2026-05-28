package com.example.leitordocumento_compose.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.leitordocumento_compose.presentation.ui.screen.Amber
import com.example.leitordocumento_compose.presentation.ui.screen.TextSec
import com.example.leitordocumento_compose.presentation.ui.theme.AppTema

// DsTextField.kt
@Composable
fun Campo(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    kb: KeyboardType = KeyboardType.Text,
    cap: KeyboardCapitalization = KeyboardCapitalization.None,
    hint: String? = null,
    alerta: Boolean = false,
    msgAlerta: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = if (alerta) Amber else TextSec, fontSize = 12.sp) },
            placeholder = hint?.let {
                {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        fontSize = 14.sp
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = kb, capitalization = cap),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.onSurface,
                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = if (alerta) Amber else com.example.leitordocumento_compose.presentation.ui.theme.AccentBlue,
                unfocusedBorderColor = if (alerta) Amber.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                focusedLabelColor = if (alerta) Amber else com.example.leitordocumento_compose.presentation.ui.theme.AccentBlue,
                unfocusedLabelColor = if (alerta) Amber else TextSec,
                cursorColor = com.example.leitordocumento_compose.presentation.ui.theme.AccentBlue
            )
        )
        if (alerta && !msgAlerta.isNullOrBlank()) {
            Text(
                "⚠ $msgAlerta", color = Amber, fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun COmponentePreview() {
    AppTema {
        Campo(
            value = "Samuel Viana",
            onValueChange = {},
            label = "Email",
        )
    }

}