package com.openpaperlabs.bolimoli.feature.mascot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** What Kili is doing. She is only active when touched ([TALKING], while she helps) or after a success ([CHEER]). */
enum class KiliMood { IDLE, TALKING, CHEER }

/** Width / height of the drawing, so callers can size her with `height(..).aspectRatio(KILI_ASPECT)`. */
const val KILI_ASPECT = 130f / 150f

// The drawing is made on a grid where she stands at about x 140..240, y 120..250. This box is what is shown.
private const val ART_LEFT = 125f
private const val ART_TOP = 100f
private const val ART_W = 130f
private const val ART_H = 150f

private val INDIGO = Color(0xFF3B2F8F)
private val SPARKLES = listOf(
    Color(0xFFFFD25E), Color(0xFFF26B5B), Color(0xFF3A86D9), Color(0xFFE85D9E), Color(0xFF2E9E5B),
    Color(0xFF8A5BD0), Color(0xFF1FA6A0)
)

/**
 * Kili the parrot, drawn on a canvas.
 *
 *  - [KiliMood.IDLE]: still, blinking now and then.
 *  - [KiliMood.TALKING]: beak opens and closes, head bobs, sound waves pulse on both sides.
 *  - [KiliMood.CHEER]: she hops out of the book, wings flapping, with confetti around her.
 *
 * Pass [onClick] to make her touchable; she squeezes a little while pressed.
 */
@Composable
fun KiliMascot(
    mood: KiliMood,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    description: String = "Kili the parrot. Touch her for help."
) {
    // 0..1, repeating while she is talking or cheering; resting at 0 when idle
    val phase = remember { Animatable(0f) }
    LaunchedEffect(mood) {
        if (mood == KiliMood.IDLE) {
            phase.snapTo(0f)
        } else {
            val duration = if (mood == KiliMood.TALKING) 900 else 1000
            while (true) {
                phase.snapTo(0f)
                phase.animateTo(1f, tween(duration, easing = LinearEasing))
            }
        }
    }

    // 1 = eyes open, 0 = closed
    val eyeOpen = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2600L + Random.nextLong(0L, 2200L))
            eyeOpen.animateTo(0f, tween(70))
            eyeOpen.animateTo(1f, tween(100))
        }
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squeeze by animateFloatAsState(if (pressed) 0.92f else 1f, label = "kiliPress")

    val touch = if (onClick != null) {
        Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .semantics { this.contentDescription = description }
            .then(touch)
    ) {
        val fit = minOf(size.width / ART_W, size.height / ART_H) * squeeze
        withTransform({
            translate((size.width - ART_W * fit) / 2f, (size.height - ART_H * fit) / 2f)
            scale(fit, fit, Offset.Zero)
            translate(-ART_LEFT, -ART_TOP)
        }) {
            drawKili(mood, phase.value, eyeOpen.value)
        }
    }
}

private fun DrawScope.drawKili(mood: KiliMood, phase: Float, eyeOpen: Float) {
    val talking = mood == KiliMood.TALKING
    val cheering = mood == KiliMood.CHEER
    val twoPi = (2.0 * PI).toFloat()

    // Sound waves on both sides of the head while she talks
    if (talking) {
        for (i in 0..2) {
            val r = 16f + 10f * i
            val alpha = 0.3f + 0.7f * ((sin((phase * 3f - i * 0.33f) * twoPi) + 1f) / 2f)
            val stroke = Stroke(width = 4f, cap = StrokeCap.Round)
            drawArc(
                color = INDIGO.copy(alpha = alpha), startAngle = -40f, sweepAngle = 80f, useCenter = false,
                topLeft = Offset(212f - r, 166f - r), size = Size(2f * r, 2f * r), style = stroke
            )
            drawArc(
                color = INDIGO.copy(alpha = alpha), startAngle = 140f, sweepAngle = 80f, useCenter = false,
                topLeft = Offset(168f - r, 166f - r), size = Size(2f * r, 2f * r), style = stroke
            )
        }
    }

    val jump = if (cheering) -abs(sin(phase * PI.toFloat() * 2f)) * 16f else 0f
    val flap = if (cheering) (sin(phase * twoPi * 3f) + 1f) / 2f * 35f else 0f
    val headBob = if (talking) sin(phase * twoPi * 2f) * 1.6f else 0f
    val beakOpen = if (talking) (sin(phase * twoPi * 3f) + 1f) / 2f else 0f

    translate(0f, jump) {
        // wings
        val wing = Brush.verticalGradient(
            colors = listOf(Color(0xFF3FAE5D), Color(0xFF1B7336)), startY = 182f, endY = 218f
        )
        rotate(flap, Offset(166f, 184f)) {
            drawOval(brush = wing, topLeft = Offset(151f, 182f), size = Size(18f, 36f))
        }
        rotate(-flap, Offset(214f, 184f)) {
            drawOval(brush = wing, topLeft = Offset(211f, 182f), size = Size(18f, 36f))
        }

        // body and belly
        drawOval(
            brush = Brush.radialGradient(
                0f to Color(0xFFA4F2B6), 0.55f to Color(0xFF46C26B), 1f to Color(0xFF26864A),
                center = Offset(182f, 184f), radius = 43f
            ),
            topLeft = Offset(164f, 167f), size = Size(52f, 56f)
        )
        drawOval(color = Color(0xFFB8EBA0), topLeft = Offset(175f, 182f), size = Size(30f, 36f))

        translate(0f, headBob) {
            // crest
            val crest = Path().apply {
                moveTo(182f, 135f)
                quadraticBezierTo(186f, 120f, 192f, 133f)
                quadraticBezierTo(196f, 122f, 200f, 135f)
                close()
            }
            drawPath(crest, Color(0xFF2A9448))

            // head with a soft highlight
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color(0xFFA4F2B6), 0.55f to Color(0xFF46C26B), 1f to Color(0xFF26864A),
                    center = Offset(181.6f, 148.8f), radius = 44.8f
                ),
                radius = 28f, center = Offset(190f, 160f)
            )
            drawOval(Color.White.copy(alpha = 0.35f), Offset(167f, 136f), Size(20f, 10f))

            // eyes: they close in a blink
            val ry = 7f * eyeOpen.coerceAtLeast(0.08f)
            drawOval(Color.White, Offset(167f, 150f - ry), Size(14f, 2f * ry))
            drawOval(Color.White, Offset(199f, 150f - ry), Size(14f, 2f * ry))
            if (eyeOpen > 0.45f) {
                drawCircle(INDIGO, 3.5f, Offset(175f, 151f))
                drawCircle(INDIGO, 3.5f, Offset(205f, 151f))
                drawCircle(Color.White, 1.2f, Offset(176f, 149.5f))
                drawCircle(Color.White, 1.2f, Offset(206f, 149.5f))
            }

            // cheeks
            drawCircle(Color(0xFFF7A8B8), 4f, Offset(168f, 166f))
            drawCircle(Color(0xFFF7A8B8), 4f, Offset(212f, 166f))

            // beak: the tip drops a little and a dark mouth shows while it is open
            val tipY = 180f + 2.5f * beakOpen
            val beak = Path().apply {
                moveTo(182f, 161f)
                quadraticBezierTo(190f, 154f, 198f, 161f)
                quadraticBezierTo(199f, 172f, 190f, tipY)
                quadraticBezierTo(181f, 172f, 182f, 161f)
                close()
            }
            drawPath(
                beak,
                Brush.verticalGradient(listOf(Color(0xFFFF9482), Color(0xFFD2432F)), startY = 154f, endY = tipY)
            )
            if (beakOpen > 0.15f) {
                drawOval(
                    Color(0xFF7A1F14),
                    Offset(190f - 4.5f * beakOpen, 166f),
                    Size(9f * beakOpen, 6f * beakOpen)
                )
            }
        }

        // confetti while she cheers
        if (cheering) {
            for (i in SPARKLES.indices) {
                val angle = (i * 360f / SPARKLES.size + phase * 60f) * (PI.toFloat() / 180f)
                val reach = 40f + 18f * phase
                drawCircle(
                    color = SPARKLES[i].copy(alpha = 1f - 0.6f * phase),
                    radius = 3.2f * (1f - 0.4f * phase),
                    center = Offset(190f + cos(angle) * reach, 165f + sin(angle) * reach * 0.9f)
                )
            }
        }
    }

    drawBook()
}

/** The open book she sits behind. */
private fun DrawScope.drawBook() {
    val cover = Path().apply {
        moveTo(146f, 214f)
        lineTo(146f, 242f)
        cubicTo(164f, 237f, 178f, 238f, 190f, 244f)
        cubicTo(202f, 238f, 216f, 237f, 234f, 242f)
        lineTo(234f, 214f)
        close()
    }
    drawPath(cover, Brush.verticalGradient(listOf(Color(0xFF5E50C8), Color(0xFF271D6B)), startY = 214f, endY = 244f))

    val left = Path().apply {
        moveTo(190f, 212f)
        cubicTo(178f, 206f, 160f, 206f, 150f, 210f)
        lineTo(150f, 232f)
        cubicTo(164f, 228f, 178f, 229f, 190f, 235f)
        close()
    }
    drawPath(left, Brush.horizontalGradient(listOf(Color.White, Color(0xFFD5CFEE)), startX = 150f, endX = 190f))

    val right = Path().apply {
        moveTo(190f, 212f)
        cubicTo(202f, 206f, 220f, 206f, 230f, 210f)
        lineTo(230f, 232f)
        cubicTo(216f, 228f, 202f, 229f, 190f, 235f)
        close()
    }
    drawPath(right, Brush.horizontalGradient(listOf(Color.White, Color(0xFFD5CFEE)), startX = 230f, endX = 190f))

    // a soft shadow where she sits
    drawOval(Color(0xFF271D6B).copy(alpha = 0.18f), Offset(164f, 208f), Size(52f, 8f))

    val lines = Color(0xFFB9B3E6)
    drawLine(lines, Offset(157f, 218f), Offset(182f, 218f), 3.5f, StrokeCap.Round)
    drawLine(lines, Offset(157f, 225f), Offset(182f, 225f), 3.5f, StrokeCap.Round)
    drawLine(lines, Offset(198f, 218f), Offset(223f, 218f), 3.5f, StrokeCap.Round)
    drawLine(Color(0xFF2E9E5B), Offset(198f, 225f), Offset(223f, 225f), 3.5f, StrokeCap.Round)
}

/**
 * Kili's speech bubble: the parts she is saying (the one being said lit up) or a short [note].
 * Draws nothing when there is neither.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KiliBubble(speech: KiliSpeech?, note: String?, modifier: Modifier = Modifier) {
    if (speech == null && note == null) return
    Surface(
        modifier = modifier.widthIn(max = 260.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = BorderStroke(2.dp, Color(0xFFD0BCFF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (speech != null) {
                Text(
                    text = speech.caption,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF79747E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    speech.parts.forEachIndexed { index, part ->
                        val active = index == speech.activePart
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) Color(0xFFFFB020) else Color(0xFFF3EDF7)
                        ) {
                            Text(
                                text = part,
                                fontSize = if (active) 26.sp else 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color(0xFF3E2723) else Color(0xFF21005D),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else if (note != null) {
                Text(
                    text = note,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}

/** Kili as a touch target bound to [helper]. */
@Composable
fun KiliMascotButton(
    helper: KiliHelperState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 84.dp
) {
    KiliMascot(
        mood = helper.mood,
        modifier = modifier
            .height(height)
            .aspectRatio(KILI_ASPECT),
        onClick = onTap
    )
}

/** Kili with her bubble above her, for placing over a screen's corner. */
@Composable
fun KiliCorner(
    helper: KiliHelperState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    mascotHeight: Dp = 84.dp
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        KiliBubble(helper.speech, helper.note)
        Spacer(modifier = Modifier.height(4.dp))
        KiliMascotButton(helper, onTap, height = mascotHeight)
    }
}

/** A row for the flow of a screen: the bubble fills the space to the left of Kili. */
@Composable
fun KiliHelperRow(
    helper: KiliHelperState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    mascotHeight: Dp = 84.dp
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            KiliBubble(helper.speech, helper.note)
        }
        Spacer(modifier = Modifier.width(8.dp))
        KiliMascotButton(helper, onTap, height = mascotHeight)
    }
}

/** Kili cheering for a finished lesson, test or story. Touch her to make her cheer again. */
@Composable
fun KiliCheer(modifier: Modifier = Modifier, height: Dp = 120.dp) {
    var tick by remember { mutableIntStateOf(0) }
    var mood by remember { mutableStateOf(KiliMood.CHEER) }
    LaunchedEffect(tick) {
        mood = KiliMood.CHEER
        delay(KiliHelperState.CHEER_MS)
        mood = KiliMood.IDLE
    }
    KiliMascot(
        mood = mood,
        modifier = modifier
            .height(height)
            .aspectRatio(KILI_ASPECT),
        onClick = { tick++ }
    )
}
