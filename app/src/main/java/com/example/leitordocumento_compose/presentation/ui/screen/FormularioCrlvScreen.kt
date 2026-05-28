package com.example.leitordocumento_compose.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.leitordocumento_compose.R
import com.example.leitordocumento_compose.presentation.ui.components.Campo
import com.example.leitordocumento_compose.presentation.ui.theme.AccentBlue

data class DadosCRLV(
    val placa: String? = null,
    val renavam: String? = null,
    val chassi: String? = null,
    val proprietario: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val anoFabricacao: String? = null,
    val anoModelo: String? = null,
    val cor: String? = null,
    val municipio: String? = null,
    val categoria: String? = null,
    val exercicio: String? = null,
    val validade: String? = null,
    val rawText: String = ""
)

@Composable
fun FormularioCrlvScreen(
    id: Long? = null,
    dados: DadosCRLV?,
    onConfirm: (DadosCRLV) -> Unit,
    onReler: () -> Unit
) {

    //TODO: REFATORAR DADOS PARA PEGAR DO ROOM E NAO DO OBJETO

    var placa by remember { mutableStateOf(dados?.placa ?: "") }
    var renavam by remember { mutableStateOf(dados?.renavam ?: "") }
    var chassi by remember { mutableStateOf(dados?.chassi ?: "") }
    var proprietario by remember { mutableStateOf(dados?.proprietario ?: "") }
    var marca by remember { mutableStateOf(dados?.marca ?: "") }
    var modelo by remember { mutableStateOf(dados?.modelo ?: "") }
    var anoFab by remember { mutableStateOf(dados?.anoFabricacao ?: "") }
    var anoMod by remember { mutableStateOf(dados?.anoModelo ?: "") }
    var cor by remember { mutableStateOf(dados?.cor ?: "") }
    var municipio by remember { mutableStateOf(dados?.municipio ?: "") }
    var validade by remember { mutableStateOf(dados?.validade ?: "") }

    FormularioScaffold(
        titulo = "CRLV detectado",
        badge = "CRLV",
        onReler = onReler,
        onConfirm = {
            onConfirm(
                DadosCRLV(
                    placa = placa.blankToNull(), renavam = renavam.blankToNull(),
                    chassi = chassi.blankToNull(), proprietario = proprietario.blankToNull(),
                    marca = marca.blankToNull(), modelo = modelo.blankToNull(),
                    anoFabricacao = anoFab.blankToNull(), anoModelo = anoMod.blankToNull(),
                    cor = cor.blankToNull(), municipio = municipio.blankToNull(),
                    validade = validade.blankToNull(), rawText = dados?.rawText ?: ""
                )
            )
        }
    ) {
        SecaoCard("Veículo") {
            Campo(
                "Placa",
                placa,
                { placa = it.uppercase() },
                cap = KeyboardCapitalization.Characters
            )
            Campo("RENAVAM", renavam, { renavam = it }, kb = KeyboardType.Number)
            Campo(
                "Chassi",
                chassi,
                { chassi = it.uppercase() },
                cap = KeyboardCapitalization.Characters
            )
            Campo("Marca", marca, { marca = it }, cap = KeyboardCapitalization.Words)
            Campo("Modelo", modelo, { modelo = it }, cap = KeyboardCapitalization.Words)
            Campo(
                "Ano fabricação",
                anoFab,
                { anoFab = it },
                kb = KeyboardType.Number,
                hint = "AAAA"
            )
            Campo("Ano modelo", anoMod, { anoMod = it }, kb = KeyboardType.Number, hint = "AAAA")
            Campo("Cor", cor, { cor = it }, cap = KeyboardCapitalization.Words)
            Campo("Município", municipio, { municipio = it }, cap = KeyboardCapitalization.Words)
        }

        SecaoCard("Proprietário e licenciamento") {
            Campo(
                "Proprietário",
                proprietario,
                { proprietario = it },
                cap = KeyboardCapitalization.Words
            )
            Campo(
                "Validade", validade, { validade = it },
                kb = KeyboardType.Number, hint = "DD/MM/AAAA",
                msgAlerta = "Verifique — licenciamento vencido ou próximo"
            )
        }
    }
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
                        color = Color(0xFF1A2740),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            badge, color = AccentBlue,
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
                            color = MaterialTheme.colorScheme.primary,
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
