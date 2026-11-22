package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Original Priyo Azan Islamic geometric Rub el Hizb emblem with crescent moon
 * and minaret acoustic wave arcs rendered natively in Canvas.
 */
@Composable
fun PriyoAzanEmblem(
    modifier: Modifier = Modifier,
    starColor: Color = SoftGoldAccent,
    innerColor: Color = Color(0xFFF7F5EE),
    ringAlpha: Float = 0.25f
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.44f

        // Subtle outer halo ring
        drawCircle(
            color = starColor.copy(alpha = ringAlpha),
            radius = r * 1.08f,
            center = Offset(cx, cy),
            style = Stroke(width = size.minDimension * 0.02f)
        )

        // Two rotated overlapping squares forming an 8-point Rub el Hizb star
        val sqSize = r * 1.32f
        val topLeft = Offset(cx - sqSize / 2f, cy - sqSize / 2f)
        val stroke = Stroke(width = size.minDimension * 0.055f)

        drawRect(
            color = starColor,
            topLeft = topLeft,
            size = Size(sqSize, sqSize),
            style = stroke
        )
        rotate(degrees = 45f, pivot = Offset(cx, cy)) {
            drawRect(
                color = starColor,
                topLeft = topLeft,
                size = Size(sqSize, sqSize),
                style = stroke
            )
        }

        // Crescent moon
        val moonR = r * 0.42f
        drawCircle(
            color = innerColor,
            radius = moonR,
            center = Offset(cx - moonR * 0.08f, cy)
        )
        drawCircle(
            color = HeroGradientMid,
            radius = moonR * 0.80f,
            center = Offset(cx + moonR * 0.25f, cy - moonR * 0.12f)
        )

        // Subtle acoustic Azan arcs
        for (i in 1..2) {
            val arcR = moonR * (0.55f + i * 0.28f)
            drawArc(
                color = starColor.copy(alpha = 0.9f - i * 0.2f),
                startAngle = -55f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(cx + moonR * 0.08f - arcR, cy - arcR),
                size = Size(arcR * 2f, arcR * 2f),
                style = Stroke(width = size.minDimension * 0.03f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Draws a subtle repeating 8-fold Islamic geometric star tessellation across a Canvas.
 */
fun DrawScope.drawIslamicGeometricLattice(
    lineColor: Color,
    cellSizePx: Float = 110f
) {
    val cols = (size.width / cellSizePx).toInt() + 2
    val rows = (size.height / cellSizePx).toInt() + 2
    val stroke = Stroke(width = 1.4f)

    for (row in 0..rows) {
        for (col in 0..cols) {
            val cx = col * cellSizePx + if (row % 2 == 1) cellSizePx * 0.5f else 0f
            val cy = row * cellSizePx
            val radius = cellSizePx * 0.34f
            drawEightPointStar(
                center = Offset(cx, cy),
                outerRadius = radius,
                innerRadius = radius * 0.48f,
                color = lineColor,
                stroke = stroke
            )
        }
    }
}

private fun DrawScope.drawEightPointStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
    stroke: Stroke
) {
    val path = Path()
    val points = 16
    for (i in 0 until points) {
        val angle = (i * 2.0 * PI / points) - PI / 2.0
        val rad = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (rad * cos(angle)).toFloat()
        val y = center.y + (rad * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color, style = stroke)
}
