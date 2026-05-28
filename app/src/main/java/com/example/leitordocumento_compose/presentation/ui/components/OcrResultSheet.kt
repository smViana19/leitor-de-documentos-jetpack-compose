package com.example.leitordocumento_compose.presentation.ui.components

import ResultadoPlaca
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.leitordocumento_compose.data.DadosCNH
import com.example.leitordocumento_compose.data.DadosRG
import com.example.leitordocumento_compose.data.OcrResultado

private val CardBg = Color(0xFF111827)
private val AccentBlue = Color(0xFF4A90D9)
private val TextPrim = Color(0xFFFFFFFF)
private val TextSec = Color(0xFF8A9BB5)
private val DividerCol = Color(0xFF1E2D45)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrResultadoSheet(
    resultado: OcrResultado,
    onDismiss: () -> Unit,
    onConfirm: (OcrResultado) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0F1A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (resultado) {
                            is OcrResultado.Cnh -> "CNH Detectada"
                            is OcrResultado.Rg -> "RG Detectado"
                            else -> "Documento Lido"
                        },
                        color = TextPrim,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Verifique os dados extraídos",
                        color = TextSec,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1A2740), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (resultado) {
                            is OcrResultado.Cnh -> "CNH"
                            is OcrResultado.Rg -> "RG"
                            else -> "?"
                        },
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            when (resultado) {
                is OcrResultado.Cnh -> CnhCampos(resultado.dadosCNH)
                is OcrResultado.Rg -> RgCampos(resultado.dadosRG)
                is OcrResultado.Placa -> PlacaCampo(resultado.dadosPlaca)
                is OcrResultado.Desconhecido -> RawTextCard(resultado.rawText)

            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSec)
                ) {
                    Text("Reler")
                }
                Button(
                    onClick = { onConfirm(resultado) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Confirmar", color = TextPrim)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}
@Composable
private fun CnhCampos(dados: DadosCNH) {
    FieldCard {
        OcrField("Nome",             dados.nome)
        OcrField("CPF",              dados.cpf)
        OcrField("RG",               dados.rg)
        OcrField("Data Nascimento",  dados.dataNascimento)
        OcrField("Nº Registro",      dados.numeroRegistro)
        OcrField("Orgao Emissor", dados.orgaoEmissor)
        OcrField("Filiacao", dados.filiacao)
        OcrField("Primeira Habilitacao", dados.primeiraHabilitacao)
        OcrField("Data Emissão",     dados.dataEmissao)
        OcrField("Validade",         dados.dataValidade)
        OcrField("Categoria",        dados.categoria)
        OcrField("Localidade",       dados.localidade)
    }
}
@Composable
private fun RgCampos(data: DadosRG) {
    FieldCard {
        OcrField("Nome",            data.nome)
        OcrField("RG",              data.rg)
        OcrField("CPF",             data.cpf)
        OcrField("Data Nascimento", data.dataNascimento)
        OcrField("Nome da Mãe",     data.nomeMae)
        OcrField("Nome do Pai",     data.nomePai)
        OcrField("Naturalidade",    data.naturalidade)
        OcrField("Data Emissão",    data.dataEmissao)
    }
}

@Composable
private fun PlacaCampo(data: ResultadoPlaca)
{
    FieldCard {
        OcrField("Placa", data.placa)
        OcrField("Placa Normalizada", data.placaNormalizada)
    }
}

@Composable
private fun FieldCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun RawTextCard(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            "Texto bruto extraído",
            color = TextSec,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text, color = TextPrim, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun OcrField(label: String, value: String?) {
    if (value == null) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = TextSec, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = TextPrim, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DividerCol, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

