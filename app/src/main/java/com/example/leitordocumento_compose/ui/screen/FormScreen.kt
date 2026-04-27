package com.example.leitordocumento_compose.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.leitordocumento_compose.R
import com.example.leitordocumento_compose.ui.theme.AppTema
import com.example.leitordocumento_compose.ui.theme.Contorno

@Composable
fun FormScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
    ) {
        Cabecalho()
        Spacer(modifier = Modifier.height(24.dp))
        ConfirmarDadosTitulo()
    }
}

@Composable
private fun Cabecalho() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {}) {
            Icon(
                painter = painterResource(R.drawable.ic_voltar),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {}) {
            Icon(
                painter = painterResource(R.drawable.settings_menu),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary
            )
        }

    }
}

@Composable
fun ConfirmarDadosTitulo(modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(), // ← deixa a altura ser natural da imagem
                painter = painterResource(R.drawable.card_img),
                contentDescription = "",
                contentScale = ContentScale.FillWidth
            )
        }
        Text(text = "Confirmar dados", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Extraímos as informações da sua CNH automaticamente.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@Composable
private fun Formulario(modifier: Modifier = Modifier) {
    var text = ""
    Column() {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Label") }
        )
        Spacer(modifier = Modifier.height(32.dp))
        BasicTextField(value = "", onValueChange = {})

    }
}


@Preview
@Composable
private fun FormScreenScreenPreview() {
    AppTema {
        FormScreen()
    }
}

@Preview
@Composable
private fun CabecalhoPreview() {
    AppTema {
        Cabecalho()
    }
}

@Preview
@Composable
private fun ConfirmarDadosTituloPreview() {
    AppTema {
        ConfirmarDadosTitulo()
    }
}

@Preview
@Composable
private fun FormularioPreview() {
    AppTema {
        Formulario()
    }
}