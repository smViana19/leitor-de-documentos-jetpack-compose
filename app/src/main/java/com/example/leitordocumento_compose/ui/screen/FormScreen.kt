package com.example.leitordocumento_compose.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ShouldPauseCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.leitordocumento_compose.R
import com.example.leitordocumento_compose.ui.theme.AppTema
import com.example.leitordocumento_compose.ui.theme.Contorno

@Composable
fun FormScreen(modifier: Modifier = Modifier)
{
    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())
        .systemBarsPadding()
    ) {
        Cabecalho()
        Spacer(modifier = Modifier.height(16.dp))
        ConfirmarDadosTitulo()
        Spacer(modifier = Modifier.height(24.dp))
        Formulario()
        Spacer(modifier = Modifier.height(8.dp))
        AlertaInformativo()
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(modifier = Modifier.padding(24.dp))
        BotaoPrincipal()
    }
}
@Composable
private fun Cabecalho()
{
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick =  {}) {
            Icon(painter = painterResource(R.drawable.ic_voltar), contentDescription = "", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {}) {
            Icon(painter = painterResource(R.drawable.settings_menu),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primary)
        }

    }
}

@Composable
fun ConfirmarDadosTitulo(modifier: Modifier = Modifier)
{
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)) {
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
        Text(text= "Confirmar dados", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text= "Extraímos as informações da sua CNH automaticamente.", style = MaterialTheme.typography.bodyMedium)
    }
}


@Composable
private fun Formulario(modifier: Modifier = Modifier)
{
    var nome by remember { mutableStateOf("")}
    var cpf by remember { mutableStateOf("")}
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)) {
        DsTextField(
            value = nome,
            onValueChange = { nome = it },
            placeholder = "Digite seu nome",
            label = "Nome Completo",
        )
        Spacer(modifier = Modifier.height(24.dp))
        DsTextField(
            value = cpf,
            onValueChange = { cpf = it },
            placeholder = "Digite seu CPF",
            label = "CPF",
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            DsTextField(
                modifier = Modifier.weight(1f),
                value = cpf,
                onValueChange = { cpf = it },
                placeholder = "RG",
                label = "RG",
            )
            Spacer(modifier = Modifier.width(16.dp))
            DsTextField(
                modifier = Modifier.weight(1f),
                value = cpf,
                onValueChange = { cpf = it },
                placeholder = "12/02/2026",
                label = "Data de Validade",
            )
        }

    }
  }

// DsTextField.kt
@Composable
private fun DsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(modifier = modifier) {

        // Label
        if (label != null) {
            Text(
                text = label,
                style = typography.labelMedium,
                color = when {
                    isError  -> colors.error
                    !enabled -> colors.onSurface.copy(alpha = 0.38f)
                    else     -> colors.onSurfaceVariant
                },
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Input
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            textStyle = typography.bodyLarge.copy(
                color = if (enabled) colors.onSurface
                else colors.onSurface.copy(alpha = 0.38f)
            ),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)) // usa o shape do seu tema
                        .background(
                            when
                            {
                                isError -> colors.errorContainer
                                !enabled -> colors.onSurface.copy(alpha = 0.04f)
                                else -> colors.surfaceVariant // ← cinza claro da imagem
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (leadingIcon != null) {
                        leadingIcon()
                        Spacer(Modifier.width(8.dp))
                    }

                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = typography.bodyLarge,
                                color = colors.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }

                    if (trailingIcon != null) {
                        Spacer(Modifier.width(8.dp))
                        trailingIcon()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Erro
        AnimatedVisibility(visible = isError && errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                style = typography.labelSmall,
                color = colors.error,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun AlertaInformativo(modifier: Modifier = Modifier)
{
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.ic_informacao_24), tint = MaterialTheme.colorScheme.onSecondaryContainer ,contentDescription = "")
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Verifique se todos campos estão preenchidos corretamente antes de salvas seus dados no cofre digital",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
fun BotaoPrincipal(modifier: Modifier = Modifier)
{
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Button(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            onClick = {}
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Salvar dados",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary)
                Icon(painter = painterResource(R.drawable.ic_salvar_24),
                    contentDescription = "")
            }
        }
    }
}


@Preview
@Composable
private fun FormScreenScreenPreview()
{
    AppTema {
        FormScreen()
    }
}

@Preview
@Composable
private fun CabecalhoPreview()
{
    AppTema {
        Cabecalho()
    }
}

@Preview
@Composable
private fun ConfirmarDadosTituloPreview()
{
    AppTema {
        ConfirmarDadosTitulo()
    }
}

@Preview
@Composable
private fun FormularioPreview()
{
    AppTema {
        Formulario()
    }
}

@Preview
@Composable
private fun DsTextFieldPreview()
{
    var nome = "Samuel Filipe Viana"
    AppTema {
        DsTextField(
            value = nome,
            onValueChange = { nome = it },
            label = "Nome Completo",
        )
    }
}

@Preview
@Composable
private fun AlertaInformativoPreview()
{
    AppTema {
        AlertaInformativo()
    }
}


@Preview
@Composable
private fun BotaoPrincipalPreview()
{
    AppTema {
        BotaoPrincipal()
    }
}