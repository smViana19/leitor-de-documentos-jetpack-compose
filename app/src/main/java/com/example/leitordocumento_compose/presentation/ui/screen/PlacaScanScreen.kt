package com.example.leitordocumento_compose.presentation.ui.screen

import PlacaOcrProcessador
import TipoVeiculo
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.documentscan.DocumentType
import com.example.documentscan.LockScreenOrientation
import com.example.documentscan.TopFloatingControls
import com.example.documentscan.drawBarraQualidade
import com.example.documentscan.drawScanCornersCentered
import com.example.documentscan.rememberTtsManager
import com.example.leitordocumento_compose.R
import com.example.leitordocumento_compose.presentation.ui.components.OcrResultadoSheet
import com.example.leitordocumento_compose.presentation.ui.states.EstadoDocumento
import com.example.leitordocumento_compose.presentation.ui.states.FeedbackDocumento
import com.example.leitordocumento_compose.presentation.ui.theme.AccentBlue
import com.example.leitordocumento_compose.presentation.ui.theme.AccentBlueBright
import com.example.leitordocumento_compose.utils.OcrProcessador
import com.example.leitordocumento_compose.data.OcrResultado
import com.example.leitordocumento_compose.data.local.repository.AppRepository
import com.example.leitordocumento_compose.presentation.ui.navigation.navegarParaFormulario
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlacaScanScreen(
    navController: NavController,
    tipoVeiculo: TipoVeiculo,
    onHelp: () -> Unit = {}
) {

    val ttsManager = rememberTtsManager()

    if (tipoVeiculo == TipoVeiculo.CARRO) {
        LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE)
    } else {
        LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT)

    }
    LaunchedEffect(tipoVeiculo) {
        if (tipoVeiculo == TipoVeiculo.CARRO) {
            delay(400)
            ttsManager.falar("Modo carro ativado. Vire o dispositivo na horizontal.")
        } else {
            delay(400)

            ttsManager.falar("Modo moto ativado. Mantenha o dispositivo na vertical")
        }
    }

    var ocrResultado by remember { mutableStateOf<OcrResultado?>(null) }
    var isProcessando by remember { mutableStateOf(false) }
    var framesPerfeitos by remember { mutableIntStateOf(0) }
    var feedbackDocumento by remember { mutableStateOf(FeedbackDocumento()) }

    val scope = rememberCoroutineScope()
    val repository = remember { AppRepository.fromAppContainer() }

    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val dispararVibracao = {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                contexto.getSystemService(Activity.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            contexto.getSystemService(Activity.VIBRATOR_SERVICE) as Vibrator
        }

        // Uma batida firme e rápida (efeito de "click" de câmera)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100) // 100 milissegundos para aparelhos antigos
        }
    }

    val previewView: PreviewView = remember {
        PreviewView(contexto).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val cornerPulse by rememberInfiniteTransition(label = "corner_pulse").animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "corner_alpha"
    )


    val ocrProcessador = remember {
        OcrProcessador(
            contexto = contexto,
            lifecycleOwner = lifecycleOwner,
            tipoDocumentoSelecionado = { DocumentType.DOCUMENT },
            onResultado = { resultado ->
                isProcessando = false
                scope.launch {
                    val id = when (val salvo = repository.salvarResultadoOcr(resultado)) {
                        is AppRepository.Salvo.Cnh -> salvo.id
                        is AppRepository.Salvo.Placa -> salvo.id
                        is AppRepository.Salvo.Rg -> salvo.id
                        is AppRepository.Salvo.Crlv -> salvo.id
                    }
                    navController.navegarParaFormulario(resultado, id)

                }
            },
            onError = { error ->
                isProcessando = false
            },
            processadorCustom = { textoOcr ->
                val placa = PlacaOcrProcessador.processarPlaca(
                    textoOcr = textoOcr,
                    tipoVeiculoHint = tipoVeiculo
                )
                if (placa != null) OcrResultado.Placa(placa)
                else OcrResultado.Desconhecido(textoOcr)
            },
            onFrameTexto = { textoFrame ->
                val placaDetectada = PlacaOcrProcessador.processarPlaca(textoFrame, tipoVeiculo)
                placaDetectada != null
            }
        ).also {
            it.bindCamera(previewView) { feedback ->
                feedbackDocumento = feedback
                if (feedback.estado != EstadoDocumento.NENHUM) {
                    framesPerfeitos++
                } else {
                    framesPerfeitos = 0
                }
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        PlacaScanOverlay(
            modifier = Modifier.fillMaxSize(),
            cornerAlpha = cornerPulse,
            feedbackDocumento = feedbackDocumento,
            tipoVeiculo = tipoVeiculo
        )

        TopFloatingControls(
            onClose = { navController.navigateUp() },
            onHelp = onHelp,
            flashOn = false,
            onFlashToggle = {}
        )
        if (!isProcessando) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 36.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Toque para capturar",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(Color(0x66000000), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        )
                        FloatingActionButton(
                            onClick = {
                                if (!isProcessando) {
                                    isProcessando = true
                                    framesPerfeitos = 0
                                    ttsManager.falar("Capturando placa")
                                    dispararVibracao()
                                    ocrProcessador.capturarEProcessar()
                                }
                            },
                            containerColor = AccentBlue,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_scanner),
                                contentDescription = "Capturar placa",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }


        if (isProcessando) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue)
            }
        }

        // Sheet de resultado de placa
//        ocrResultado?.let { resultado ->
//            OcrResultadoSheet(
//                resultado = resultado,
//                onDismiss = { ocrResultado = null },
//                onConfirm = { confirmedResult ->
//                    ocrResultado = null
//
//                }
//            )
//        }
    }
}

@Composable
private fun PlacaScanOverlay(
    modifier: Modifier = Modifier,
    cornerAlpha: Float,
    feedbackDocumento: FeedbackDocumento,
    tipoVeiculo: TipoVeiculo
) {
    val proporcao = if (tipoVeiculo == TipoVeiculo.MOTO) 0.56f else 3.08f
    val labelTipo = if (tipoVeiculo == TipoVeiculo.MOTO) "Placa de Moto" else "Placa de Carro"


    val label = if (tipoVeiculo == TipoVeiculo.MOTO)
        "Posicione a placa da moto no guia"
    else
        "Posicione a placa do carro no guia"

    val corAnimada by animateColorAsState(
        targetValue = feedbackDocumento.corOverlay,
        animationSpec = tween(400),
        label = "cor_overlay"
    )

    Box(
        modifier = modifier
            .graphicsLayer { alpha = 0.99f }
            .drawWithContent {
                drawContent()

                val guideHeight = size.height * 0.55f
                val guideWidth = guideHeight * proporcao
                    .coerceAtMost(size.width * 0.88f / guideHeight * guideHeight) // não ultrapassa a tela
                val topLeftX = (size.width - guideWidth) / 2
                val topLeftY = (size.height - guideHeight) / 2

                val guideTopLeft = Offset(topLeftX, topLeftY)
                val guideSize = Size(guideWidth, guideHeight)

                drawRect(color = Color(0x88000000))
                drawRect(
                    color = Color.Transparent,
                    topLeft = guideTopLeft,
                    size = guideSize,
                    blendMode = BlendMode.Clear
                )
                drawScanCornersCentered(
                    color = AccentBlueBright.copy(alpha = cornerAlpha),
                    cornerLength = 36.dp.toPx(),
                    strokeWidth = 4.dp.toPx(),
                    cornerRadius = 10.dp.toPx(),
                    guideTopLeft = guideTopLeft,
                    guideSize = guideSize
                )

                if (feedbackDocumento.progresso > 0f) {
                    drawBarraQualidade(feedbackDocumento.progresso, corAnimada)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Posicione a $labelTipo no guia",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .background(Color(0x99000000), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}