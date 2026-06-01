package com.example.leitordocumento_compose.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.leitordocumento_compose.R
import com.example.leitordocumento_compose.data.DadosCNH
import com.example.leitordocumento_compose.data.local.repository.AppRepository
import com.example.leitordocumento_compose.presentation.ui.components.Campo
import com.example.leitordocumento_compose.presentation.ui.theme.AppTema

@Composable
fun FormularioCnhScreen(
    id: Long,
    dados: DadosCNH?,
    onConfirm: (DadosCNH) -> Unit,
    onReler: () -> Unit
) {

    val repository = remember { AppRepository.fromAppContainer() }
    var dados by remember { mutableStateOf<DadosCNH?>(null) }

    LaunchedEffect(id) {
        val entity = repository.buscarCnh(id)
        dados = entity?.let {
            DadosCNH(
                nome = it.nome,
                cpf = it.cpf,
                rg = it.rg,
                orgaoEmissor = it.orgaoEmissor,
                numeroRegistro = it.numeroRegistro,
                categoria = it.categoria,
                primeiraHabilitacao = it.primeiraHabilitacao,
                dataEmissao = it.dataEmissao,
                dataValidade = it.dataValidade,
                dataNascimento = it.dataNascimento,
                localNascimento = it.localNascimento,
                filiacao = it.filiacao,
                rawText = it.rawText ?: ""
            )
        }
    }

    if(dados == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var nome            by remember(dados) { mutableStateOf(dados?.nome ?: "") }
    var cpf             by remember(dados) { mutableStateOf(dados?.cpf ?: "") }
    var rg              by remember(dados) { mutableStateOf(dados?.rg ?: "") }
    var orgaoEmissor    by remember(dados) { mutableStateOf(dados?.orgaoEmissor ?: "") }
    var dataNascimento  by remember(dados) { mutableStateOf(dados?.dataNascimento ?: "") }

    FormularioScaffold(
        titulo = "CNH detectada",
        badge = "CNH",
        onReler = onReler,
        onConfirm = {
            onConfirm(
                DadosCNH(
                    nome = nome.blankToNull(), cpf = cpf.blankToNull(),
                    rg = rg.blankToNull(), orgaoEmissor = orgaoEmissor.blankToNull(),
                    dataNascimento = dataNascimento.blankToNull(),
                    rawText = dados?.rawText ?: ""
                )
            )
        }
    ) {
        SecaoCard("Identificação") {
            Campo("Nome completo", nome, { nome = it }, cap = KeyboardCapitalization.Words)
            Campo("CPF", cpf, { cpf = it }, kb = KeyboardType.Number, hint = "000.000.000-00")
            Campo("RG / Doc. identidade", rg, { rg = it })
            Campo("Órgão emissor", orgaoEmissor, { orgaoEmissor = it }, cap = KeyboardCapitalization.Characters)
            Campo("Data de nascimento", dataNascimento, { dataNascimento = it },
                kb = KeyboardType.Number, hint = "DD/MM/AAAA")
        }
    }
}


private fun validadeProxima(data: String): Boolean {
    if (data.isBlank()) return false
    return try {
        val p = data.split(Regex("[/.-]"))
        if (p.size != 3) return false
        val cal = java.util.Calendar.getInstance().apply {
            set(p[2].toInt(), p[1].toInt() - 1, p[0].toInt())
        }
        (cal.timeInMillis - System.currentTimeMillis()) / 86_400_000L <= 90
    } catch (_: Exception) { false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioScaffold(
    titulo: String,
    badge: String,
    onReler: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    IconButton(onClick = onReler) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fechar_24),
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                title = {
                    Column {
                        Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Revise e corrija se necessário",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp
                        )
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            badge, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReler,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_scanner),
                            contentDescription = null, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Reler")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Confirmar dados",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun String.blankToNull(): String? = ifBlank { null }

@Preview(showBackground = true)
@Composable
private fun FormularioCnhScreenPreview()
{
    AppTema {
        FormularioCnhScreen(1, null, {}, {})
    }
}

