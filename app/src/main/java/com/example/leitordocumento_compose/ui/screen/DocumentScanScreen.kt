package com.example.documentscan

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposablePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.leitordocumento_compose.R

// ──────────────────────────────────────────────
// Cores
// ──────────────────────────────────────────────
private val BackgroundTop = Color(0xFF0A1628)
private val BackgroundBottom = Color(0xFF0D1F3C)
private val BottomPanel = Color(0xFF0A0F1A)
private val AccentBlue = Color(0xFF4A90D9)
private val AccentBlueBright = Color(0xFF5BA3F0)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF8A9BB5)
private val IndicatorActive = Color(0xFF4A90D9)
private val OverlayScrim = Color(0x99000000)

// ──────────────────────────────────────────────
// Modelo
// ──────────────────────────────────────────────
enum class DocumentType(val label: String) {
    ID_CARD("ID CARD"),
    DOCUMENT("DOCUMENT"),
    BOOK("BOOK"),
    PASSPORT("PASSPORT")
}

// ──────────────────────────────────────────────
// Tela principal
// ──────────────────────────────────────────────
@Composable
fun DocumentScanScreen(
    onClose: () -> Unit = {},
    onHelp: () -> Unit = {},
    onCapture: () -> Unit = {},
    onFlashToggle: () -> Unit = {},
    onSwitchCamera: () -> Unit = {}
) {
    var selectedType by remember { mutableStateOf(DocumentType.DOCUMENT) }
    var zoom by remember { mutableFloatStateOf(1f) }
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundTop, BackgroundBottom),
                    startY = 0f,
                    endY = 800f
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Barra superior ──────────────────────────
            TopBar(onClose = onClose, onHelp = onHelp)

            // ── Viewfinder (câmera + overlay) ───────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Fundo escuro atrás do viewfinder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF060D1A))
                )

                // Preview da câmera
                CameraPreviewBox(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .fillMaxHeight(0.78f)
                        .clip(RoundedCornerShape(12.dp))
                )

                // Overlay com cantos animados e texto
                ScanOverlay(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .fillMaxHeight(0.78f),
                    cornerAlpha = cornerPulse
                )
            }

            // ── Painel inferior ──────────────────────────
            BottomControls(
                zoom = zoom,
                onZoomChange = { zoom = it },
                flashOn = flashOn,
                onFlashToggle = {
                    flashOn = !flashOn
                    onFlashToggle()
                },
                onCapture = onCapture,
                onSwitchCamera = onSwitchCamera,
                selectedType = selectedType,
                onTypeSelected = { selectedType = it }
            )
        }
    }
}

// ──────────────────────────────────────────────
// Barra superior
// ──────────────────────────────────────────────
@Composable
private fun TopBar(onClose: () -> Unit, onHelp: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Botão fechar
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .background(Color(0xFF1A2740), RoundedCornerShape(12.dp))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_salvar_24),
                contentDescription = "Fechar",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Título centralizado
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DOCUMENT SCAN",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Indicador pulsante
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
                        .size(6.dp)
                        .background(
                            IndicatorActive.copy(alpha = pulse),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "AUTO-DETECT ACTIVE",
                    color = IndicatorActive,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // Botão ajuda
        IconButton(
            onClick = onHelp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(44.dp)
                .background(Color(0xFF1A2740), RoundedCornerShape(12.dp))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_duvida_24),
                contentDescription = "Ajuda",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────
// Preview da câmera (CameraX)
// ──────────────────────────────────────────────
@Composable
private fun CameraPreviewBox(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
// ──────────────────────────────────────────────
// Overlay do scanner (cantos + texto)
// ──────────────────────────────────────────────
@Composable
private fun ScanOverlay(
    modifier: Modifier = Modifier,
    cornerAlpha: Float
) {
    Box(
        modifier = modifier
            .drawWithContent {
                drawContent()
                drawScanCorners(
                    color = AccentBlueBright.copy(alpha = cornerAlpha),
                    cornerLength = 32.dp.toPx(),
                    strokeWidth = 3.dp.toPx(),
                    cornerRadius = 12.dp.toPx()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "ALIGN DOCUMENT WITHIN FRAME",
            color = TextPrimary.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun DrawScope.drawScanCorners(
    color: Color,
    cornerLength: Float,
    strokeWidth: Float,
    cornerRadius: Float
) {
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    val pad = strokeWidth / 2

    // Top-left
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(pad, pad),
        size = Size(cornerRadius * 2, cornerRadius * 2),
        style = stroke
    )
    drawLine(color, Offset(pad + cornerRadius, pad), Offset(pad + cornerRadius + cornerLength, pad), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(pad, pad + cornerRadius), Offset(pad, pad + cornerRadius + cornerLength), strokeWidth, StrokeCap.Round)

    // Top-right
    drawArc(
        color = color,
        startAngle = 270f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(size.width - cornerRadius * 2 - pad, pad),
        size = Size(cornerRadius * 2, cornerRadius * 2),
        style = stroke
    )
    drawLine(color, Offset(size.width - pad - cornerRadius, pad), Offset(size.width - pad - cornerRadius - cornerLength, pad), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(size.width - pad, pad + cornerRadius), Offset(size.width - pad, pad + cornerRadius + cornerLength), strokeWidth, StrokeCap.Round)

    // Bottom-left
    drawArc(
        color = color,
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(pad, size.height - cornerRadius * 2 - pad),
        size = Size(cornerRadius * 2, cornerRadius * 2),
        style = stroke
    )
    drawLine(color, Offset(pad + cornerRadius, size.height - pad), Offset(pad + cornerRadius + cornerLength, size.height - pad), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(pad, size.height - pad - cornerRadius), Offset(pad, size.height - pad - cornerRadius - cornerLength), strokeWidth, StrokeCap.Round)

    // Bottom-right
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(size.width - cornerRadius * 2 - pad, size.height - cornerRadius * 2 - pad),
        size = Size(cornerRadius * 2, cornerRadius * 2),
        style = stroke
    )
    drawLine(color, Offset(size.width - pad - cornerRadius, size.height - pad), Offset(size.width - pad - cornerRadius - cornerLength, size.height - pad), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(size.width - pad, size.height - pad - cornerRadius), Offset(size.width - pad, size.height - pad - cornerRadius - cornerLength), strokeWidth, StrokeCap.Round)
}

// ──────────────────────────────────────────────
// Controles inferiores
// ──────────────────────────────────────────────
@Composable
private fun BottomControls(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    flashOn: Boolean,
    onFlashToggle: () -> Unit,
    onCapture: () -> Unit,
    onSwitchCamera: () -> Unit,
    selectedType: DocumentType,
    onTypeSelected: (DocumentType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BottomPanel)
            .padding(top = 20.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom slider
        ZoomSlider(zoom = zoom, onZoomChange = onZoomChange)

        Spacer(modifier = Modifier.height(20.dp))

        // Botões de ação
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash
            ActionButton(
                icon = painterResource(R.drawable.ic_flash_24),
                label = if (flashOn) "FLASH ON" else "FLASH OFF",
                onClick = onFlashToggle
            )

            // Botão capturar (central, maior)
            CaptureButton(onClick = onCapture)

            ActionButton(
                icon = painterResource(R.drawable.ic_flash_24),
                label = if (flashOn) "FLASH ON" else "FLASH OFF",
                onClick = onFlashToggle
            )
            // Trocar câmera
//            ActionButton(
//                icon = Icons.Default.FlipCameraAndroid,
//                label = "SWITCH",
//                onClick = onSwitchCamera
//            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tabs de tipo de documento
        DocumentTypeTabs(
            selectedType = selectedType,
            onTypeSelected = onTypeSelected
        )
    }
}

// ──────────────────────────────────────────────
// Zoom Slider
// ──────────────────────────────────────────────
@Composable
private fun ZoomSlider(zoom: Float, onZoomChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("1X", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Slider(
            value = zoom,
            onValueChange = onZoomChange,
            valueRange = 1f..5f,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = AccentBlue,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = Color(0xFF2A3A55)
            )
        )
        Text("5X", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ──────────────────────────────────────────────
// Botão de ação lateral (flash / switch)
// ──────────────────────────────────────────────
@Composable
private fun ActionButton(
    icon: Painter,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color(0xFF1A2740), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = label,
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

// ──────────────────────────────────────────────
// Botão capturar (central)
// ──────────────────────────────────────────────
@Composable
private fun CaptureButton(onClick: () -> Unit) {
    val scale by rememberInfiniteTransition(label = "capture_scale").animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_val"
    )

    Box(
        modifier = Modifier
            .size(76.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(AccentBlueBright, AccentBlue),
                    radius = 100f
                ),
                RoundedCornerShape(22.dp)
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF7EC8FF), AccentBlue)
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_salvar_24), // substituir por ícone de câmera real
            contentDescription = "Capturar",
            tint = TextPrimary,
            modifier = Modifier.size(30.dp)
        )
    }
}

// ──────────────────────────────────────────────
// Tabs de tipo de documento
// ──────────────────────────────────────────────
@Composable
private fun DocumentTypeTabs(
    selectedType: DocumentType,
    onTypeSelected: (DocumentType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DocumentType.entries.forEach { type ->
            val isSelected = type == selectedType
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTypeSelected(type) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = type.label,
                    color = if (isSelected) AccentBlueBright else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(5.dp))
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(2.dp)
                            .background(AccentBlueBright, RoundedCornerShape(1.dp))
                    )
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// Preview (sem câmera real)
// ──────────────────────────────────────────────
@ComposablePreview(showBackground = true, backgroundColor = 0xFF0A1628)
@Composable
private fun DocumentScanScreenPreview() {
    DocumentScanScreen()
}