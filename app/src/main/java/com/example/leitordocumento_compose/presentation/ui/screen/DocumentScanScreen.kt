package com.example.documentscan

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.leitordocumento_compose.R
import com.example.leitordocumento_compose.presentation.ui.components.OcrResultadoSheet
import com.example.leitordocumento_compose.presentation.ui.states.EstadoDocumento
import com.example.leitordocumento_compose.presentation.ui.states.FeedbackDocumento
import com.example.leitordocumento_compose.presentation.ui.theme.AccentBlue
import com.example.leitordocumento_compose.presentation.ui.theme.AccentBlueBright
import com.example.leitordocumento_compose.presentation.ui.theme.AppTema
import com.example.leitordocumento_compose.presentation.ui.theme.IndicatorActive
import com.example.leitordocumento_compose.presentation.ui.theme.TextPrimary
import com.example.leitordocumento_compose.utils.OcrProcessador
import com.example.leitordocumento_compose.utils.OcrResultado
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview as ComposablePreview


enum class DocumentType(val label: String)
{
    ID_CARD("ID CARD"),
    DOCUMENT("DOCUMENT"),
    BOOK("BOOK"),
    PASSPORT("PASSPORT")
}

@Composable
fun LockScreenOrientation(orientation: Int)
{
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose {
            activity.requestedOrientation = originalOrientation
        }
    }
}


@Composable
fun DocumentScanScreen(
    navController: NavController,
    onHelp: () -> Unit = {},
)
{
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE)
    var feedbackDocumento by remember { mutableStateOf(FeedbackDocumento()) }


    var ocrResultado by remember { mutableStateOf<OcrResultado?>(null) }
    var isProcessando by remember { mutableStateOf(false) }
    var framesPerfeitos by remember { mutableIntStateOf(0) }
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView: PreviewView = remember {
        PreviewView(contexto).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val ocrProcessador = remember {
        OcrProcessador(
            contexto = contexto,
            lifecycleOwner = lifecycleOwner,
            tipoDocumentoSelecionado = { DocumentType.DOCUMENT },
            onResultado = { resultado ->
                isProcessando = false
                ocrResultado = resultado
            },
            onError = { error ->
                isProcessando = false
                Toast.makeText(contexto, "Erro ao processar $error", Toast.LENGTH_LONG)
                    .show()
            }
        ).also {
            it.bindCamera(previewView) { feedback ->
                feedbackDocumento = feedback
                if(feedback.estado == EstadoDocumento.PERFEITO) framesPerfeitos++
                else framesPerfeitos = 0
            }
        }
    }
    LaunchedEffect(framesPerfeitos) {
        if (framesPerfeitos >= 4 && !isProcessando) {
            delay(500)   // 500ms adicionais de segurança
            if (framesPerfeitos >= 4 && !isProcessando) {
                isProcessando = true
                framesPerfeitos = 0
                ocrProcessador.capturarEProcessar()
            }
        }
    }

    var flashOn by remember { mutableStateOf(false) }

    // Animação pulsante dos cantos
    val cornerPulse by rememberInfiniteTransition(label = "corner_pulse").animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corner_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Câmera ocupando toda a tela
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Overlay desenhado por cima da câmera
        ScanOverlay(
            modifier = Modifier.fillMaxSize(),
            cornerAlpha = cornerPulse,
            feedback = feedbackDocumento
        )

        TopFloatingControls(
            onClose = { navController.navigateUp() },
            onHelp = onHelp,
            flashOn = flashOn,
            onFlashToggle = { flashOn = ocrProcessador.ligarFlash(flashOn) }
        )

        if (!isProcessando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!isProcessando) {
                            isProcessando = true
                            framesPerfeitos = 0
                            ocrProcessador.capturarEProcessar()
                        }
                    },
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_scanner),
                        contentDescription = "Capturar manualmente"
                    )
                }
            }
        }

        if (isProcessando)
        {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue)
            }
        }

        ocrResultado?.let { resultado ->
            OcrResultadoSheet(
                resultado = resultado,
                onDismiss = { ocrResultado = null },
                onConfirm = { confirmedResult ->
                    ocrResultado = null

                }
            )
        }
    }

}

@Composable
fun TopFloatingControls(
    onClose: () -> Unit,
    onHelp: () -> Unit,
    flashOn: Boolean,
    onFlashToggle: () -> Unit
)
{
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        // Botão Fechar (Esquerda)
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .background(Color(0x66000000), CircleShape)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_fechar_24),
                contentDescription = "Fechar",
                tint = TextPrimary
            )
        }

        // Indicador de Auto Captura (Centro)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color(0x66000000), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val pulse by rememberInfiniteTransition(label = "dot_pulse").animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_alpha"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(IndicatorActive.copy(alpha = pulse), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Auto detecção ativa",
                color = IndicatorActive,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Botões Flash e Ajuda (Direita)
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0x66000000), CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_flash_24), // Substitua se tiver ícones de flash on/off
                    contentDescription = "Flash",
                    tint = if (flashOn) AccentBlueBright else TextPrimary
                )
            }
            IconButton(
                onClick = onHelp,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0x66000000), CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_duvida_24),
                    contentDescription = "Ajuda",
                    tint = TextPrimary
                )
            }
        }
    }
}


@Composable
private fun ScanOverlay(
    modifier: Modifier = Modifier,
    cornerAlpha: Float,
    feedback: FeedbackDocumento = FeedbackDocumento()
)
{
    val corAnimada by animateColorAsState(
        targetValue = feedback.corOverlay,
        animationSpec = tween(400),
        label = "cor_overlay"
    )

    Box(
        modifier = modifier
            // A MÁGICA AQUI: Cria uma camada isolada para o Compose.
            // Sem isso, o BlendMode.Clear vai apagar a câmera e mostrar o fundo preto!
            .graphicsLayer { alpha = 0.99f }
            .drawWithContent {
                drawContent()

                // Dimensões do retângulo guia no centro da tela
                val guideWidth = size.width * 0.75f
                val guideHeight = size.height * 0.65f
                val topLeftX = (size.width - guideWidth) / 2
                val topLeftY = (size.height - guideHeight) / 2
                val guideTopLeft = Offset(topLeftX, topLeftY)
                val guideSize = Size(guideWidth, guideHeight)

                // 1. Desenha a camada escura sobre TUDO
                drawRect(color = Color(0x66000000))

                // 2. Funciona como um "furador de papel": apaga o centro, revelando a câmera que está na camada de baixo
                drawRect(
                    color = Color.Transparent,
                    topLeft = guideTopLeft,
                    size = guideSize,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                // 3. Desenha os cantos do guia de escaneamento
                drawScanCornersCentered(
                    color = AccentBlueBright.copy(alpha = cornerAlpha),
                    cornerLength = 40.dp.toPx(),
                    strokeWidth = 4.dp.toPx(),
                    cornerRadius = 16.dp.toPx(),
                    guideTopLeft = guideTopLeft,
                    guideSize = guideSize
                )

                if (feedback.progresso > 0f)
                {
                    drawBarraQualidade(
                        progresso = feedback.progresso,
                        cor = corAnimada
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EstadoIcone(estado = feedback.estado)
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedContent(
                targetState = feedback.mensagem,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "mensagem_feedback"
            ) { msg ->
                if (msg.isNotEmpty())
                {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color(0x99000000), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun EstadoIcone(estado: EstadoDocumento)
{
    val (emoji, cor) = when (estado)
    {
        EstadoDocumento.NENHUM -> "" to Color(0xFF8A9BB5)
        EstadoDocumento.DETECTANDO -> "" to Color(0xFF8A9BB5)
        EstadoDocumento.ALINHANDO -> "" to Color(0xFF4A90D9)
        EstadoDocumento.PERFEITO -> "" to Color(0xFF1D9E75)
        EstadoDocumento.RUIM_LUZ -> "" to Color(0xFFBA7517)
        EstadoDocumento.DESFOCADO -> "" to Color(0xFFBA7517)
    }
    Text(text = emoji, fontSize = 22.sp)
}

fun DrawScope.drawScanCornersCentered(
    color: Color,
    cornerLength: Float,
    strokeWidth: Float,
    cornerRadius: Float,
    guideTopLeft: Offset,
    guideSize: Size
)
{
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

    val left = guideTopLeft.x
    val top = guideTopLeft.y
    val right = guideTopLeft.x + guideSize.width
    val bottom = guideTopLeft.y + guideSize.height

    // Top-left
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(left, top),
        size = Size(cornerRadius * 2, cornerRadius * 2),
        style = stroke
    )
    drawLine(color,
        Offset(left + cornerRadius, top),
        Offset(left + cornerRadius + cornerLength, top),
        strokeWidth,
        StrokeCap.Round)
    drawLine(color,
        Offset(left, top + cornerRadius),
        Offset(left, top + cornerRadius + cornerLength),
        strokeWidth,
        StrokeCap.Round)

    // Top-right
    drawArc(
        color = color,
        startAngle = 270f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(right - cornerRadius * 2, top),
        size = Size(cornerRadius * 2, cornerRadius * 2),
        style = stroke
    )
    drawLine(color,
        Offset(right - cornerRadius, top),
        Offset(right - cornerRadius - cornerLength, top),
        strokeWidth,
        StrokeCap.Round)
    drawLine(color,
        Offset(right, top + cornerRadius),
        Offset(right, top + cornerRadius + cornerLength),
        strokeWidth,
        StrokeCap.Round)

    // Bottom-left
    drawArc(
        color = color,
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(left, bottom - cornerRadius * 2),
        size = Size(cornerRadius * 2, cornerRadius * 2),
        style = stroke
    )
    drawLine(color,
        Offset(left + cornerRadius, bottom),
        Offset(left + cornerRadius + cornerLength, bottom),
        strokeWidth,
        StrokeCap.Round)
    drawLine(color,
        Offset(left, bottom - cornerRadius),
        Offset(left, bottom - cornerRadius - cornerLength),
        strokeWidth,
        StrokeCap.Round)

    // Bottom-right
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(right - cornerRadius * 2, bottom - cornerRadius * 2),
        size = Size(cornerRadius * 2, cornerRadius * 2),
        style = stroke
    )
    drawLine(color,
        Offset(right - cornerRadius, bottom),
        Offset(right - cornerRadius - cornerLength, bottom),
        strokeWidth,
        StrokeCap.Round)
    drawLine(color,
        Offset(right, bottom - cornerRadius),
        Offset(right, bottom - cornerRadius - cornerLength),
        strokeWidth,
        StrokeCap.Round)

}

@ComposablePreview(showBackground = true, backgroundColor = 0xFF0A1628)
@Composable
private fun DocumentScanScreenPreview()
{
    val navController = rememberNavController()
    AppTema {
        DocumentScanScreen(navController)
    }
}


fun DrawScope.drawBarraQualidade(progresso: Float, cor: Color)
{
    val altBarra = 6.dp.toPx()
    val y = size.height - altBarra
    drawRect(color = Color(0x33FFFFFF), topLeft = Offset(0f, y), size = Size(size.width, altBarra))
    drawRect(color = cor, topLeft = Offset(0f, y), size = Size(size.width * progresso, altBarra))
}
