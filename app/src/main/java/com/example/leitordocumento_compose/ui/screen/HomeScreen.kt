package com.example.leitordocumento_compose.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.leitordocumento_compose.R
import com.example.leitordocumento_compose.ui.theme.AppTema

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val listaTeste = listOf("CNH", "CPF", "Outros Documentos")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
    ) {
        Cabecalho()
        Spacer(modifier.height(32.dp))
        Titulo()
        Spacer(modifier = Modifier.height(40.dp))
        CardPrincipal()
        Spacer(modifier = Modifier.height(24.dp))
        CardSelecao()
        Spacer(modifier = Modifier.height(24.dp))
        HistoricoDigitalizacoes(listaTeste)
    }
}

@Composable
private fun Cabecalho(modifier: Modifier = Modifier) {
    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically

    ) {
        IconButton(onClick = {}) {
            Icon(
                painter = painterResource(R.drawable.menu_icone),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier.width(16.dp))
        Text(
            text = "Scanner Digital",
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
fun Titulo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Text(
            text = "INÍCIO",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Novo Escaneamento",
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
        )
    }
}

@Composable
fun CardPrincipal() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSecondary)
    ) {

        Column(modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(R.drawable.ic_scanner), contentDescription = "")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "RG",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Escaneie frente e verso do seu Documento de Identidade.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {

                Button(
                    modifier = Modifier, shape = RoundedCornerShape(12.dp),
                    onClick = {}) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Iniciar",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_proximo_24),
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun CardSelecao(modifier: Modifier = Modifier) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSecondary)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.ic_carro_24), contentDescription = "")
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column() {
                Text(text = "CNH", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Carteira de motorista", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

}

@Composable
fun HistoricoDigitalizacoes(lista: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Histórico", style = MaterialTheme.typography.headlineLarge)
            TextButton(onClick = {}) {
                Text(text = "Ver tudo", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(lista) { item ->
                CardImagemHistorico()
            }

        }

    }
}

@Composable
fun CardImagemHistorico(modifier: Modifier = Modifier) {
    Card(
        modifier = Modifier.size(160.dp, 218.dp).padding(end = 16.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSecondary)
    ) {}
}


@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AppTema {
        HomeScreen()
    }
}

@Preview
@Composable
private fun CardSelecaoPreview() {
    AppTema {
        CardSelecao()
    }
}

@Preview
@Composable
private fun HistoricoDigitalizacoesPreview() {
    AppTema {
        val listaMockada = listOf("a", "b", "c")
        HistoricoDigitalizacoes(listaMockada)
    }
}


@Preview
@Composable
private fun CardImagemHistoricoPreview() {
    AppTema {
        CardImagemHistorico()
    }

}