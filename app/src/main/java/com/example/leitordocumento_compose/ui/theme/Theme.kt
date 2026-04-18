package com.example.leitordocumento_compose.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimariaEscuro,
    onPrimary = SobrePrimariaEscuro,
    primaryContainer = ContainerPrimariaEscuro,
    onPrimaryContainer = SobreContainerPrimariaEscuro,

    secondary = SecundariaEscuro,
    onSecondary = SobreSecundariaEscuro,
    secondaryContainer = ContainerSecundariaEscuro,
    onSecondaryContainer = SobreContainerSecundariaEscuro,

    tertiary = TerciariaEscuro,
    onTertiary = SobreTerciariaEscuro,
    tertiaryContainer = ContainerTerciariaEscuro,
    onTertiaryContainer = SobreContainerTerciariaEscuro,

    error = ErroEscuro,
    onError = SobreErroEscuro,
    errorContainer = ContainerErroEscuro,
    onErrorContainer = SobreContainerErroEscuro,

    background = FundoEscuro,
    onBackground = SobreFundoEscuro,

    surface = SuperficieEscuro,
    onSurface = SobreSuperficieEscuro,
    surfaceVariant = VarianteSuperficieEscuro,
    onSurfaceVariant = SobreVarianteSuperficieEscuro,

    surfaceContainerLowest = ContainerSuperficieMaisAbaixoEscuro,
    surfaceContainerLow = ContainerSuperficieAbaixoEscuro,
    surfaceContainer = ContainerSuperficieEscuro,
    surfaceContainerHigh = ContainerSuperficieAcimaEscuro,
    surfaceContainerHighest = ContainerSuperficieMaisAcimaEscuro,

    outline = ContornoEscuro,
    outlineVariant = VarianteContornoEscuro,

    inverseSurface = SuperficieInversaEscuro,
    inverseOnSurface = SobreSuperficieInversaEscuro,
    inversePrimary = PrimariaInversaEscuro,

    surfaceTint = TinteSuperficieEscuro,
    scrim = Veu,
)

private val LightColorScheme = lightColorScheme(
    primary = Primaria,
    onPrimary = SobrePrimaria,
    primaryContainer = ContainerPrimaria,
    onPrimaryContainer = SobreContainerPrimaria,

    secondary = Secundaria,
    onSecondary = SobreSecundaria,
    secondaryContainer = ContainerSecundaria,
    onSecondaryContainer = SobreContainerSecundaria,

    tertiary = Terciaria,
    onTertiary = SobreTerciaria,
    tertiaryContainer = ContainerTerciaria,
    onTertiaryContainer = SobreContainerTerciaria,

    error = Erro,
    onError = SobreErro,
    errorContainer = ContainerErro,
    onErrorContainer = SobreContainerErro,

    background = Fundo,
    onBackground = SobreFundo,

    surface = Superficie,
    onSurface = SobreSuperficie,
    surfaceVariant = VarianteSuperficie,
    onSurfaceVariant = SobreVarianteSuperficie,

    surfaceContainerLowest = ContainerSuperficieMaisAbaixo,
    surfaceContainerLow = ContainerSuperficieAbaixo,
    surfaceContainer = ContainerSuperficie,
    surfaceContainerHigh = ContainerSuperficieAcima,
    surfaceContainerHighest = ContainerSuperficieMaisAcima,

    outline = Contorno,
    outlineVariant = VarianteContorno,

    inverseSurface = SuperficieInversa,
    inverseOnSurface = SobreSuperficieInversa,
    inversePrimary = PrimariaInversa,

    surfaceTint = TinteSuperficie,
    scrim = Veu,

)

@Composable
fun AppTema(
    temaEscuro: Boolean = isSystemInDarkTheme(),
    corDinamica: Boolean = false,
    content: @Composable () -> Unit
)
{
    val colorScheme = when
    {
        corDinamica && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        {
            val context = LocalContext.current
            if (temaEscuro) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        temaEscuro -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}