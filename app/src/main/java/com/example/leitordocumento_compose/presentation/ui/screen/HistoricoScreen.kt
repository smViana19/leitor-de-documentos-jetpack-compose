package com.example.leitordocumento_compose.presentation.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.leitordocumento_compose.R
import com.example.leitordocumento_compose.data.local.repository.AppRepository
import com.example.leitordocumento_compose.presentation.ui.theme.AppTema


data class DocumentoHistorico(
    val id: Long,
    val tipo: String,
    val descricao: String,
    val dataScaneado: String,
    val iconRes: Int,
)

data class PlacaHistorico(
    val id: Long,
    val placa: String,
    val modelo: String,
    val dataScaneado: String,
    val iconRes: Int,
)

// ─── Tela principal ──────────────────────────────────────────────────────────

@Composable
fun HistoricoScreen(
    navController: NavController
) {

    val repository = remember { AppRepository.fromAppContainer() }

    var documentos by remember { mutableStateOf<List<DocumentoHistorico>>(emptyList()) }
    var placas by remember { mutableStateOf<List<PlacaHistorico>>(emptyList()) }

    val cnhsEntities by repository.listarCnhs().collectAsState(initial = emptyList())
    val rgsEntities by repository.listarRgs().collectAsState(initial = emptyList())
    val crlvsEntities by repository.listarCrlvs().collectAsState(initial = emptyList())
    val placasEntities by repository.listarPlacas().collectAsState(initial = emptyList())

    LaunchedEffect(cnhsEntities, rgsEntities, crlvsEntities) {
        val listaUnificada = mutableListOf<DocumentoHistorico>()

        listaUnificada.addAll(cnhsEntities.map {
            DocumentoHistorico(
                id = it.id,
                tipo = "CNH",
                descricao = it.nome ?: "Nao informado",
                dataScaneado = it.dataEmissao ?: "Data não informada",
                iconRes = R.drawable.ic_scanner
            )
        })

        listaUnificada.addAll(rgsEntities.map {
            DocumentoHistorico(
                id = it.id,
                tipo = "RG",
                descricao = it.nome ?: "Nao informado",
                dataScaneado = it.dataEmissao ?: "Data não informada",
                iconRes = R.drawable.ic_scanner
            )
        })

        listaUnificada.addAll(crlvsEntities.map {
            DocumentoHistorico(
                id = it.id,
                tipo = "CRLV",
                descricao = "${it.marca} ${it.modelo}",
                dataScaneado = "Registrado", // Adapte se você tiver um campo de data na Entity
                iconRes = R.drawable.ic_scanner
            )
        })

        // Ordena por ID (do mais recente para o mais antigo)
        documentos = listaUnificada.sortedByDescending { it.id }
    }

    // 5. Mapeamento das Placas
    LaunchedEffect(placasEntities) {
        placas = placasEntities.map {
            PlacaHistorico(
                id = it.id,
                placa = it.placa,
                modelo = "Veículo Escaneado",
                dataScaneado = "Registrado",
                iconRes = R.drawable.ic_carro_24
            )
        }.sortedByDescending { it.id }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val abas = listOf("Documentos", "Placas")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        HistoricoCabecalho(navController = navController)

        Spacer(modifier = Modifier.height(8.dp))

        BadgeSeguranca(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab])
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {}
        ) {
            abas.forEachIndexed { index, titulo ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.padding(vertical = 4.dp),
                    text = {
                        Text(
                            text = titulo,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedTab == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 10 }) togetherWith fadeOut()
            },
            label = "tab_content"
        ) { tab ->
            when (tab) {
                0 -> ListaDocumentos(documentos = documentos, navController = navController)
                1 -> ListaPlacas(placas = placas, navController = navController)
            }
        }
    }
}

@Composable
private fun HistoricoCabecalho(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                painter = painterResource(R.drawable.ic_voltar),
                contentDescription = "Voltar",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Histórico",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Seus documentos escaneados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BadgeSeguranca(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_informacao_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Dados protegidos e seguros seguindo a LGPD",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun ListaDocumentos(
    documentos: List<DocumentoHistorico>,
    navController: NavController,
) {
    if (documentos.isEmpty()) {
        EmptyState(
            mensagem = "Nenhum documento escaneado ainda",
            submensagem = "Escaneie sua CNH, RG ou passaporte para salvá-lo aqui."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(documentos) { _, doc ->
                CardDocumento(
                    documento = doc,
                    onClick = {
                        // navController.navigate("form/${doc.id}")
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ListaPlacas(
    placas: List<PlacaHistorico>,
    navController: NavController,
) {
    if (placas.isEmpty()) {
        EmptyState(
            mensagem = "Nenhuma placa escaneada ainda",
            submensagem = "Escaneie a placa de um veículo para salvá-la aqui."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(placas) { _, placa ->
                CardPlaca(
                    placa = placa,
                    onClick = {
                        // navController.navigate("placa_form/${placa.id}")
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun CardDocumento(
    documento: DocumentoHistorico,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSecondary
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(documento.iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = documento.tipo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = documento.descricao,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Escaneado em ${documento.dataScaneado}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Seta de navegação
            Icon(
                painter = painterResource(R.drawable.ic_avancar_24), // substitua por ic_seta_direita
                contentDescription = "Ver detalhes",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
                // Dica: aplique graphicsLayer { rotationZ = 180f } se usar ic_voltar como seta
            )
        }
    }
}

// ─── Card Placa ───────────────────────────────────────────────────────────────

@Composable
fun CardPlaca(
    placa: PlacaHistorico,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSecondary
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(placa.iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = placa.placa,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = placa.modelo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Escaneado em ${placa.dataScaneado}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Seta de navegação
            Icon(
                painter = painterResource(R.drawable.ic_voltar), // substitua por ic_seta_direita
                contentDescription = "Ver detalhes",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── Badge "Salvo no cofre" ───────────────────────────────────────────────────



// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    mensagem: String,
    submensagem: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(R.drawable.ic_folder), contentDescription = "")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = mensagem,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = submensagem,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun HistoricoEmptyPreview() {
    AppTema {
        HistoricoScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoricoComDadosPreview() {
    val docs = listOf(
        DocumentoHistorico(1, "CNH", "Carteira de Motorista", "28/05/2026", R.drawable.ic_carro_24),
        DocumentoHistorico(2, "RG", "Registro Geral", "20/05/2026", R.drawable.ic_carro_24),
    )
    val placas = listOf(
        PlacaHistorico(1, "ABC-1D23", "Toyota Corolla 2022", "28/05/2026", R.drawable.ic_carro_24),
    )
    AppTema {
        HistoricoScreen(
            navController = rememberNavController(),
        )
    }
}

@Preview
@Composable
private fun CardDocumentoPreview() {
    AppTema {
        CardDocumento(
            documento = DocumentoHistorico(
                id = 1,
                tipo = "CNH",
                descricao = "Carteira de Motorista",
                dataScaneado = "28/05/2026",
                iconRes = R.drawable.ic_carro_24
            ),
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun EmptyStatePreview() {
    AppTema {
        EmptyState(
            mensagem = "Nenhum documento escaneado ainda",
            submensagem = "Escaneie sua CNH, RG ou CRLV para salvá-lo aqui."
        )
    }
}