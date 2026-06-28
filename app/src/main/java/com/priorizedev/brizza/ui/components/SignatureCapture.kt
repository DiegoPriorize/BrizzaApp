package com.priorizedev.brizza.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.priorizedev.brizza.util.ReportUtils

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    heightDp: Int = 180,
    labelText: String = "Assinatura Digital",
    onSignatureCaptured: (String) -> Unit // Base64 png
) {
    val points = remember { mutableStateListOf<Offset?>() }
    var containerWidth by remember { mutableStateOf(400) }
    var containerHeight by remember { mutableStateOf(200) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = labelText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            IconButton(
                onClick = { points.clear() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Limpar Assinatura",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            points.add(offset)
                        },
                        onDragEnd = {
                            points.add(null) // end sub-path
                            
                            // Immediately capture signature as Base64 when finger raises
                            if (points.any { it != null }) {
                                val bmp = generateSignatureBitmap(points, containerWidth, containerHeight)
                                val base64 = ReportUtils.bitmapToBase64(bmp)
                                onSignatureCaptured(base64)
                            }
                        },
                        onDragCancel = {
                            points.add(null)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            points.add(change.position)
                        }
                    )
                }
        ) {
            val density = LocalDensity.current
            LaunchedEffect(maxWidth, maxHeight) {
                containerWidth = with(density) { maxWidth.toPx().toInt() }
                containerHeight = with(density) { maxHeight.toPx().toInt() }
            }

            if (points.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Assine aqui com seu dedo",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
            
            ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    if (p1 != null && p2 != null) {
                        drawLine(
                            color = Color.Black,
                            start = p1,
                            end = p2,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

private fun generateSignatureBitmap(points: List<Offset?>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE) // White Background

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 8f
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    val path = Path()
    var isStarting = true

    for (p in points) {
        if (p == null) {
            isStarting = true
        } else {
            if (isStarting) {
                path.moveTo(p.x, p.y)
                isStarting = false
            } else {
                path.lineTo(p.x, p.y)
            }
        }
    }
    
    canvas.drawPath(path, paint)
    return bitmap
}
