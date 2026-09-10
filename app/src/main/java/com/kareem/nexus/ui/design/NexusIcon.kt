package com.kareem.nexus.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class NexusIconType { Home, Discover, Memory, Activity, Settings, Capture, Link, Image, Saved, Web }

@Composable
fun NexusIcon(
    type: NexusIconType,
    modifier: Modifier = Modifier,
    primary: Color = NexusColors.Cyan,
    secondary: Color = NexusColors.Violet,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = (size.minDimension * 0.085f).coerceAtLeast(2.dp.toPx())
        val outline = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(w * x, h * y)
        fun line(color: Color, start: Offset, end: Offset, width: Float = stroke) {
            drawLine(color = color, start = start, end = end, strokeWidth = width, cap = StrokeCap.Round)
        }

        when (type) {
            NexusIconType.Home -> {
                val path = Path().apply {
                    moveTo(w * .15f, h * .48f)
                    lineTo(w * .5f, h * .18f)
                    lineTo(w * .85f, h * .48f)
                    lineTo(w * .78f, h * .48f)
                    lineTo(w * .78f, h * .82f)
                    lineTo(w * .58f, h * .82f)
                    lineTo(w * .58f, h * .60f)
                    lineTo(w * .42f, h * .60f)
                    lineTo(w * .42f, h * .82f)
                    lineTo(w * .22f, h * .82f)
                    lineTo(w * .22f, h * .48f)
                }
                drawPath(path = path, color = primary, style = outline)
                line(secondary, p(.50f, .18f), p(.78f, .42f), stroke * .55f)
            }
            NexusIconType.Discover -> {
                drawCircle(color = primary, radius = w * .25f, center = p(.43f, .43f), style = outline)
                line(secondary, p(.61f, .61f), p(.84f, .84f))
            }
            NexusIconType.Memory -> {
                repeat(3) { i ->
                    val y = .28f + i * .22f
                    drawOval(
                        color = primary,
                        topLeft = p(.20f, y - .08f),
                        size = Size(w * .60f, h * .16f),
                        style = outline,
                    )
                    if (i < 2) {
                        line(secondary, p(.20f, y), p(.20f, y + .22f), stroke * .7f)
                        line(secondary, p(.80f, y), p(.80f, y + .22f), stroke * .7f)
                    }
                }
            }
            NexusIconType.Activity -> {
                val path = Path().apply {
                    moveTo(w * .10f, h * .56f)
                    lineTo(w * .30f, h * .56f)
                    lineTo(w * .40f, h * .26f)
                    lineTo(w * .55f, h * .78f)
                    lineTo(w * .66f, h * .44f)
                    lineTo(w * .90f, h * .44f)
                }
                drawPath(path = path, color = primary, style = outline)
                line(secondary, p(.40f, .26f), p(.55f, .78f), stroke * .55f)
            }
            NexusIconType.Settings -> {
                drawCircle(color = primary, radius = w * .16f, center = p(.5f, .5f), style = outline)
                repeat(8) { i ->
                    val a = Math.toRadians((i * 45).toDouble())
                    val c = kotlin.math.cos(a).toFloat()
                    val sn = kotlin.math.sin(a).toFloat()
                    line(
                        secondary,
                        p(.5f + c * .27f, .5f + sn * .27f),
                        p(.5f + c * .39f, .5f + sn * .39f),
                    )
                }
            }
            NexusIconType.Capture -> {
                drawRoundRect(
                    color = primary,
                    topLeft = p(.16f, .28f),
                    size = Size(w * .68f, h * .50f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .10f),
                    style = outline,
                )
                drawCircle(color = secondary, radius = w * .14f, center = p(.50f, .53f), style = outline)
                line(primary, p(.31f, .28f), p(.39f, .18f))
                line(primary, p(.39f, .18f), p(.58f, .18f))
                line(primary, p(.58f, .18f), p(.66f, .28f))
            }
            NexusIconType.Link -> {
                drawArc(
                    color = primary,
                    startAngle = 130f,
                    sweepAngle = 220f,
                    useCenter = false,
                    topLeft = p(.10f, .27f),
                    size = Size(w * .46f, h * .46f),
                    style = outline,
                )
                drawArc(
                    color = secondary,
                    startAngle = -50f,
                    sweepAngle = 220f,
                    useCenter = false,
                    topLeft = p(.44f, .27f),
                    size = Size(w * .46f, h * .46f),
                    style = outline,
                )
                line(NexusColors.TextPrimary, p(.36f, .64f), p(.64f, .36f), stroke * .65f)
            }
            NexusIconType.Image -> {
                drawRoundRect(
                    color = primary,
                    topLeft = p(.14f, .18f),
                    size = Size(w * .72f, h * .64f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .08f),
                    style = outline,
                )
                drawCircle(color = secondary, radius = w * .07f, center = p(.64f, .36f))
                val path = Path().apply {
                    moveTo(w * .22f, h * .70f)
                    lineTo(w * .42f, h * .50f)
                    lineTo(w * .54f, h * .62f)
                    lineTo(w * .66f, h * .49f)
                    lineTo(w * .78f, h * .70f)
                }
                drawPath(path = path, color = secondary, style = outline)
            }
            NexusIconType.Saved -> {
                val path = Path().apply {
                    moveTo(w * .28f, h * .18f)
                    lineTo(w * .72f, h * .18f)
                    lineTo(w * .72f, h * .82f)
                    lineTo(w * .50f, h * .67f)
                    lineTo(w * .28f, h * .82f)
                    close()
                }
                drawPath(path = path, color = primary, style = outline)
                line(secondary, p(.50f, .22f), p(.50f, .61f), stroke * .55f)
            }
            NexusIconType.Web -> {
                drawCircle(color = primary, radius = w * .34f, center = p(.5f, .5f), style = outline)
                drawOval(
                    color = secondary,
                    topLeft = p(.35f, .16f),
                    size = Size(w * .30f, h * .68f),
                    style = outline,
                )
                line(primary, p(.18f, .50f), p(.82f, .50f), stroke * .7f)
            }
        }
    }
}
