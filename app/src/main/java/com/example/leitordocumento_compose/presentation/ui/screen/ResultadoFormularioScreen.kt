package com.example.leitordocumento_compose.presentation.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.leitordocumento_compose.R
import com.example.leitordocumento_compose.data.DadosCNH
import com.example.leitordocumento_compose.data.DadosRG
import com.example.leitordocumento_compose.data.OcrResultado
import com.example.leitordocumento_compose.presentation.ui.theme.AccentBlue
import com.example.leitordocumento_compose.presentation.ui.theme.Terciaria

@Composable
fun ResultadoFormularioScreen(
    resultado: OcrResultado,
    navController: NavController,
    onConfirm: (OcrResultado) -> Unit,
    onReler: () -> Unit
) {

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FormTopBar(
                titulo = when (resultado) {
                    is OcrResultado.Cnh -> "CNH detectada"
                    is OcrResultado.Rg -> "RG detectado"
                    is OcrResultado.Placa -> "Placa detectada"
                    else -> "Documento lido"
                },
                subtitulo = "Revise e corrija os campos se necessário",
                badge = when (resultado) {
                    is OcrResultado.Cnh -> "CNH"
                    is OcrResultado.Rg -> "RG"
                    is OcrResultado.Placa -> "PLACA"
                    else -> "?"
                },
                onBack = onReler
            )
        },
        bottomBar = {
            FormBottomBar(onReler = onReler, onConfirm = { onConfirm(resultado) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (resultado) {
                is OcrResultado.Cnh -> FormularioCnh(resultado.dadosCNH) { novo ->
                    // Em produção, propague o valor editado via ViewModel/state
                }

                is OcrResultado.Rg -> FormularioRg(resultado.dadosRG) { novo -> }
                is OcrResultado.Placa -> FormularioPlaca(resultado.dadosPlaca.placa)
                is OcrResultado.Desconhecido -> RawTextSection(resultado.rawText)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}


@Composable
private fun RawTextSection(text: String) {
    SectionCard(titulo = "Texto extraído") {
        Text(
            text = text.ifBlank { "(nenhum texto reconhecido)" },
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormTopBar(
    titulo: String,
    subtitulo: String,
    badge: String,
    onBack: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = {
            Column {
                Text(
                    titulo,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(subtitulo, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_fechar_24), //Adicionar icone de voltar (loading n sei o nome)
                    contentDescription = "Reler",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .background(Color(0xFF1A2740), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(badge, color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    )
}

@Composable
private fun FormBottomBar(onReler: () -> Unit, onConfirm: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    // tint sutil na borda
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_scanner),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Reler documento")
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2740))
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


@Composable
private fun FormularioCnh(dados: DadosCNH, onChange: (DadosCNH) -> Unit) {
    SectionCard(titulo = "Identificação") {
        OcrTextField(
            label = "Nome completo",
            value = dados.nome,
            icon = "ti-user",
            capitalization = KeyboardCapitalization.Words
        )
        OcrTextField(
            label = "CPF",
            value = dados.cpf,
            icon = "ti-id",
            keyboardType = KeyboardType.Number,
            mask = "###.###.###-##"
        )
        OcrTextField(
            label = "RG / Doc. identidade",
            value = dados.rg,
            icon = "ti-id"
        )
        OcrTextField(
            label = "Órgão emissor",
            value = dados.orgaoEmissor,
            icon = "ti-building"
        )
    }

    SectionCard(titulo = "Habilitação") {
        OcrTextField(
            label = "Nº de registro",
            value = dados.numeroRegistro,
            icon = "ti-hash",
            keyboardType = KeyboardType.Number
        )
        OcrTextField(
            label = "Categoria",
            value = dados.categoria,
            icon = "ti-license",
            capitalization = KeyboardCapitalization.Characters
        )
        OcrTextField(
            label = "Primeira habilitação",
            value = dados.primeiraHabilitacao,
            icon = "ti-calendar",
            mask = "##/##/####"
        )
        OcrTextField(
            label = "Data de emissão",
            value = dados.dataEmissao,
            icon = "ti-calendar",
            mask = "##/##/####"
        )
        OcrTextField(
            label = "Validade",
            value = dados.dataValidade,
            icon = "ti-calendar-event",
            mask = "##/##/####",
            highlight = dados.dataValidade?.let { isValidadeProxima(it) } == true
        )
    }

    SectionCard(titulo = "Nascimento") {
        OcrTextField(
            label = "Data de nascimento",
            value = dados.dataNascimento,
            icon = "ti-cake",
            mask = "##/##/####"
        )
        OcrTextField(
            label = "Local de nascimento",
            value = dados.localNascimento,
            icon = "ti-map-pin"
        )
        OcrTextField(
            label = "Filiação",
            value = dados.filiacao,
            icon = "ti-users",
            capitalization = KeyboardCapitalization.Words
        )
    }
}

@Composable
private fun FormularioRg(dados: DadosRG, onChange: (DadosRG) -> Unit) {
    SectionCard(titulo = "Identificação") {
        OcrTextField(
            label = "Nome completo",
            value = dados.nome,
            icon = "ti-user",
            capitalization = KeyboardCapitalization.Words
        )
        OcrTextField(
            label = "RG",
            value = dados.rg,
            icon = "ti-id"
        )
        OcrTextField(
            label = "CPF",
            value = dados.cpf,
            icon = "ti-id",
            keyboardType = KeyboardType.Number,
            mask = "###.###.###-##"
        )
    }

    SectionCard(titulo = "Dados pessoais") {
        OcrTextField(
            label = "Data de nascimento",
            value = dados.dataNascimento,
            icon = "ti-cake",
            mask = "##/##/####"
        )
        OcrTextField(
            label = "Naturalidade",
            value = dados.naturalidade,
            icon = "ti-map-pin"
        )
        OcrTextField(
            label = "Nome da mãe",
            value = dados.nomeMae,
            icon = "ti-user",
            capitalization = KeyboardCapitalization.Words
        )
        OcrTextField(
            label = "Nome do pai",
            value = dados.nomePai,
            icon = "ti-user",
            capitalization = KeyboardCapitalization.Words
        )
    }

    SectionCard(titulo = "Emissão") {
        OcrTextField(
            label = "Data de emissão",
            value = dados.dataEmissao,
            icon = "ti-calendar",
            mask = "##/##/####"
        )
    }
}

@Composable
private fun FormularioPlaca(placa: String?) {
    SectionCard(titulo = "Placa identificada") {
        var valor by remember { mutableStateOf(placa ?: "") }
        Column(modifier = Modifier.fillMaxWidth()) {
            // Placa em destaque (fonte grande, centralizada)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valor.uppercase().ifBlank { "—" },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = valor,
                onValueChange = { valor = it.uppercase().take(8) },
                label = { Text("Corrigir placa", color = MaterialTheme.colorScheme.secondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters
                ),
                modifier = Modifier.fillMaxWidth(),

                )
        }
    }
}

@Composable
private fun SectionCard(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onSecondary, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = titulo.uppercase(),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.onSecondary, thickness = 0.5.dp)
        content()
    }
}


@Composable
private fun OcrTextField(
    label: String,
    value: String?,
    icon: String? = null,       // nome do ícone Tabler (não usado no Compose nativo — placeholder)
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    mask: String? = null,
    highlight: Boolean = false
) {
    var texto by remember(value) { mutableStateOf(value ?: "") }
    val borderColor = when {
        highlight -> Color(0xFFE8A020)   // alerta validade
        texto.isBlank() -> Color(0xFF2A3A55) // campo vazio — discreto
        else -> Terciaria
    }

    OutlinedTextField(
        value = texto,
        onValueChange = { texto = it },
        label = {
            Text(
                label,
                color = if (highlight) Color(0xFFE8A020) else MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp
            )
        },
        placeholder = mask?.let {
            {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization
        ),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.primary,
            unfocusedTextColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = borderColor,
            focusedLabelColor = AccentBlue,
            unfocusedLabelColor = if (highlight) Color(0xFFE8A020) else MaterialTheme.colorScheme.secondary,
            cursorColor = AccentBlue
        ),
        shape = RoundedCornerShape(10.dp)
    )
    if (highlight) {
        Text(
            "⚠ Verifique a data de validade",
            color = Color(0xFFE8A020),
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}

private fun isValidadeProxima(data: String): Boolean {
    return try {
        val partes = data.split(Regex("[/.-]"))
        if (partes.size != 3) return false
        val dia = partes[0].toInt();
        val mes = partes[1].toInt();
        val ano = partes[2].toInt()
        val hoje = java.util.Calendar.getInstance()
        val validade = java.util.Calendar.getInstance().apply {
            set(ano, mes - 1, dia)
        }
        val diffMs = validade.timeInMillis - hoje.timeInMillis
        diffMs < 90L * 24 * 60 * 60 * 1000
    } catch (e: Exception) {
        false
    }
}
