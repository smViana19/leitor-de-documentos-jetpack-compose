package com.example.leitordocumento_compose.presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.leitordocumento_compose.R
import com.example.leitordocumento_compose.presentation.ui.navigation.Screens
import com.example.leitordocumento_compose.presentation.ui.theme.AppTema


enum class CategoriaScanner
{
    DOCUMENTO, PLACA
}

data class TipoDocumento(
    val titulo: String,
    val descricao: String,
    val iconeRes: Int,
    val rotaDestino: String,
    val categoria: CategoriaScanner
)

@Composable
fun HomeScreen(navController: NavController)
{
    val listaTeste = listOf("CNH", "CPF", "Outros Documentos")

    var abaSelecionada by remember { mutableIntStateOf(0) }
    val titulosAbas = listOf("Documentos", "Placas")

    val listaDocumentos = listOf(
        TipoDocumento("RG",
            "Escaneie frente e verso do seu Documento de Identidade.",
            R.drawable.ic_scanner,
            Screens.TELA_SCANNER.name,
            CategoriaScanner.DOCUMENTO),
        TipoDocumento("CNH",
            "Escaneie sua Carteira Nacional de Habilitação.",
            R.drawable.ic_carro_24,
            Screens.TELA_SCANNER.name,
            CategoriaScanner.DOCUMENTO),
        TipoDocumento("CRLV",
            "Certificado de Registro e Licenciamento de Veículo.",
            R.drawable.ic_scanner,
            Screens.TELA_SCANNER.name,
            CategoriaScanner.DOCUMENTO),
        TipoDocumento("Placa de Carro",
            "Escaneie a placa do veículo (Carro).",
            R.drawable.ic_carro_24,
            "${Screens.TELA_SCANNER_PLACA.name}/CARRO",
            CategoriaScanner.PLACA),
        TipoDocumento("Placa de Moto",
            "Escaneie a placa da motocicleta.",
            R.drawable.ic_carro_24,
            "${Screens.TELA_SCANNER_PLACA.name}/MOTO",
            CategoriaScanner.PLACA)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
    ) {
        Spacer(Modifier.height(32.dp))
        Titulo()
        Spacer(modifier = Modifier.height(40.dp))
        TabRow(
            selectedTabIndex = abaSelecionada,
            modifier = Modifier.padding(horizontal = 24.dp),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            titulosAbas.forEachIndexed { index, titulo ->
                Tab(
                    selected = abaSelecionada == index,
                    onClick = { abaSelecionada = index },
                    text = {
                        Text(
                            text = titulo,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (abaSelecionada == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val documentosFiltrados = listaDocumentos.filter { documento ->
            when (abaSelecionada)
            {
                0 -> documento.categoria == CategoriaScanner.DOCUMENTO
                1 -> documento.categoria == CategoriaScanner.PLACA
                else -> false
            }
        }

        // 6. Iterando sobre a lista filtrada
        documentosFiltrados.forEach { documento ->
            CardDocumento(
                documento = documento,
                onClick = { navController.navigate(documento.rotaDestino) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        HistoricoDigitalizacoes(listaTeste)
    }
}

@Composable
private fun Cabecalho(modifier: Modifier = Modifier)
{
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
fun Titulo()
{
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
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
fun CardPrincipal(navController: NavController)
{
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
                    onClick = {
                        navController.navigate(Screens.TELA_SCANNER.name)
                    }) {
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
fun CardDocumento(
    documento: TipoDocumento,
    onClick: () -> Unit
)
{
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSecondary)
    ) {
        Column(modifier = Modifier.padding(24.dp)) { // Reduzi um pouco o padding para caberem melhor na tela
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painter = painterResource(documento.iconeRes),
                        contentDescription = documento.titulo)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = documento.titulo,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = documento.descricao,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    modifier = Modifier,
                    shape = RoundedCornerShape(12.dp),
                    onClick = onClick
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Iniciar",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_proximo_24),
                            contentDescription = "Iniciar scanner para ${documento.titulo}"
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun CardSelecao(modifier: Modifier = Modifier)
{
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
                Text(text = "Carteira de motorista",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

}

@Composable
fun HistoricoDigitalizacoes(lista: List<String>)
{
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
fun CardImagemHistorico(modifier: Modifier = Modifier)
{
    Card(
        modifier = Modifier
            .size(160.dp, 218.dp)
            .padding(end = 16.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSecondary)
    ) {}
}


@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview()
{
    val navController = rememberNavController()

    AppTema {
        HomeScreen(navController)
    }
}

@Preview
@Composable
private fun CardSelecaoPreview()
{
    AppTema {
        CardSelecao()
    }
}

@Preview
@Composable
private fun HistoricoDigitalizacoesPreview()
{
    AppTema {
        val listaMockada = listOf("a", "b", "c")
        HistoricoDigitalizacoes(listaMockada)
    }
}


@Preview
@Composable
private fun CardImagemHistoricoPreview()
{
    AppTema {
        CardImagemHistorico()
    }

}