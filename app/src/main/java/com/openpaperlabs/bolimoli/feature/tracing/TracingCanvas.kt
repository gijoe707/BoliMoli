package com.openpaperlabs.bolimoli.feature.tracing

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openpaperlabs.bolimoli.core.curriculum.NormalizedPoint
import kotlin.math.hypot
import kotlin.math.min

private enum class TracingResult {
    SUCCESS,
    TOO_SHORT,
    TOO_MUCH_OUTSIDE,
    INCOMPLETE_COVERAGE
}

private fun evaluateTracing(
    letterText: String,
    allStrokes: List<List<Offset>>,
    canvasWidth: Float,
    canvasHeight: Float
): TracingResult {
    if (allStrokes.isEmpty() || canvasWidth <= 0f || canvasHeight <= 0f) {
        return TracingResult.TOO_SHORT
    }

    // 1. Calculate total stroke distance
    var totalLen = 0f
    val allPoints = mutableListOf<Offset>()
    for (stroke in allStrokes) {
        allPoints.addAll(stroke)
        for (i in 0 until stroke.size - 1) {
            val p1 = stroke[i]
            val p2 = stroke[i + 1]
            totalLen += hypot(p2.x - p1.x, p2.y - p1.y)
        }
    }

    if (totalLen < 180f || allPoints.size < 5) {
        return TracingResult.TOO_SHORT
    }

    // 2. Build the Letter Glyph Path and Tolerance Region
    val midLineY = canvasHeight * 0.475f
    val textSizePx = min(canvasWidth, canvasHeight) * 0.52f

    val textPaint = Paint().apply {
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val fontMetrics = textPaint.fontMetrics
    val textY = midLineY + (fontMetrics.descent - fontMetrics.ascent) / 2f - fontMetrics.descent
    val textX = canvasWidth / 2f

    val glyphPath = AndroidPath()
    textPaint.getTextPath(letterText, 0, letterText.length, textX, textY, glyphPath)

    val bounds = RectF()
    glyphPath.computeBounds(bounds, true)

    // Expand path with stroke width to create tolerance corridor around letter lines
    val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 65f // Finger tolerance width
    }
    val tolerancePath = AndroidPath()
    strokePaint.getFillPath(glyphPath, tolerancePath)

    val clipRegion = Region(0, 0, canvasWidth.toInt(), canvasHeight.toInt())
    val toleranceRegion = Region()
    toleranceRegion.setPath(tolerancePath, clipRegion)

    // 3. Count On-Target vs Off-Target points
    var insideCount = 0
    var outsideCount = 0

    val numGridCols = 4
    val numGridRows = 4
    val touchedGrid = BooleanArray(numGridCols * numGridRows)

    val boundsW = (bounds.width()).coerceAtLeast(1f)
    val boundsH = (bounds.height()).coerceAtLeast(1f)

    for (p in allPoints) {
        val ix = p.x.toInt()
        val iy = p.y.toInt()
        if (toleranceRegion.contains(ix, iy)) {
            insideCount++
            val col = (((p.x - bounds.left) / boundsW) * numGridCols).toInt().coerceIn(0, numGridCols - 1)
            val row = (((p.y - bounds.top) / boundsH) * numGridRows).toInt().coerceIn(0, numGridRows - 1)
            touchedGrid[row * numGridCols + col] = true
        } else {
            outsideCount++
        }
    }

    val totalPoints = insideCount + outsideCount
    if (totalPoints == 0) return TracingResult.TOO_SHORT

    val outsideRatio = outsideCount.toFloat() / totalPoints.toFloat()

    // Reject if more than 20% of drawing points land outside the letter tolerance corridor
    if (outsideRatio > 0.20f) {
        return TracingResult.TOO_MUCH_OUTSIDE
    }

    // Reject if less than 4 grid cells of the letter bounds were covered
    val cellsTouched = touchedGrid.count { it }
    if (cellsTouched < 4) {
        return TracingResult.INCOMPLETE_COVERAGE
    }

    return TracingResult.SUCCESS
}

@Composable
fun TracingCanvas(
    letterText: String,
    templateStrokes: List<List<NormalizedPoint>> = emptyList(),
    showTemplate: Boolean = true,
    onTraceCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userPaths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isVerified by remember { mutableStateOf(false) }
    var hintVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val activeTemplate = showTemplate || hintVisible

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (showTemplate) "Trace Letter: $letterText" else "Freehand Practice: $letterText",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
                Text(
                    text = if (showTemplate) "Trace over the dashed guide outline" else "Write on your own without a template guide",
                    fontSize = 14.sp,
                    color = Color(0xFF49454F)
                )
            }

            if (!showTemplate) {
                TextButton(
                    onClick = { hintVisible = !hintVisible }
                ) {
                    Text(if (hintVisible) "Hide Hint" else "Show Hint", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Canvas container box
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF7F2FA),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath = listOf(offset)
                                    validationError = null
                                },
                                onDrag = { change, _ ->
                                    currentPath = currentPath + change.position
                                },
                                onDragEnd = {
                                    if (currentPath.isNotEmpty()) {
                                        userPaths.add(currentPath)
                                        currentPath = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    canvasSize = size
                    val width = size.width
                    val height = size.height

                    // 1. Draw Writing Baseline & Guide Lines
                    val topGuideY = height * 0.2f
                    val baselineY = height * 0.75f
                    val midLineY = height * 0.475f

                    val lineStyle = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    drawLine(Color(0xFFE0E0E0), Offset(0f, topGuideY), Offset(width, topGuideY), strokeWidth = 2f, pathEffect = lineStyle.pathEffect)
                    drawLine(Color(0xFFBDBDBD), Offset(0f, midLineY), Offset(width, midLineY), strokeWidth = 2f, pathEffect = lineStyle.pathEffect)
                    drawLine(Color(0xFF90CAF9), Offset(0f, baselineY), Offset(width, baselineY), strokeWidth = 4f)

                    // 2. Draw Template Guide Outline (Native Canvas Text Glyph Outline)
                    if (activeTemplate) {
                        val textSizePx = min(width, height) * 0.52f
                        val textPaint = Paint().apply {
                            textSize = textSizePx
                            color = android.graphics.Color.argb(120, 103, 80, 164)
                            textAlign = Paint.Align.CENTER
                            style = Paint.Style.STROKE
                            strokeWidth = 8f
                            pathEffect = DashPathEffect(floatArrayOf(16f, 12f), 0f)
                            isAntiAlias = true
                            typeface = Typeface.DEFAULT_BOLD
                        }

                        val fontMetrics = textPaint.fontMetrics
                        val textY = midLineY + (fontMetrics.descent - fontMetrics.ascent) / 2f - fontMetrics.descent
                        drawContext.canvas.nativeCanvas.drawText(
                            letterText,
                            width / 2f,
                            textY,
                            textPaint
                        )
                    }

                    // 3. Draw User Traced Lines
                    val allUserStrokes = userPaths + listOfNotNull(currentPath.takeIf { it.isNotEmpty() })
                    for (userStroke in allUserStrokes) {
                        if (userStroke.size >= 2) {
                            val path = Path()
                            path.moveTo(userStroke[0].x, userStroke[0].y)
                            for (i in 1 until userStroke.size) {
                                path.lineTo(userStroke[i].x, userStroke[i].y)
                            }
                            drawPath(
                                path = path,
                                color = if (isVerified) Color(0xFF2E7D32) else Color(0xFF6750A4),
                                style = Stroke(
                                    width = 28f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                // In Freehand mode, show a small reference badge in the top right corner
                if (!showTemplate) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8DEF8),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6750A4))
                    ) {
                        Text(
                            text = letterText,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6750A4)
                        )
                    }
                }
            }
        }

        if (validationError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = validationError!!,
                color = Color(0xFFC62828),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = {
                    userPaths.clear()
                    currentPath = emptyList()
                    isVerified = false
                    validationError = null
                },
                modifier = Modifier.height(48.dp)
            ) {
                Text("Clear", fontSize = 18.sp)
            }

            Button(
                onClick = {
                    val allStrokes = userPaths + listOfNotNull(currentPath.takeIf { it.isNotEmpty() })
                    val result = evaluateTracing(
                        letterText = letterText,
                        allStrokes = allStrokes,
                        canvasWidth = canvasSize.width,
                        canvasHeight = canvasSize.height
                    )

                    when (result) {
                        TracingResult.SUCCESS -> {
                            validationError = null
                            isVerified = true
                            onTraceCompleted()
                        }
                        TracingResult.TOO_SHORT -> {
                            validationError = "Please trace more of the letter before checking!"
                        }
                        TracingResult.TOO_MUCH_OUTSIDE -> {
                            validationError = "Too far outside! Stay along the letter outline."
                        }
                        TracingResult.INCOMPLETE_COVERAGE -> {
                            validationError = "Keep going! Cover more of the letter outline to finish."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6750A4)
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Check", fontSize = 18.sp)
            }
        }
    }
}
