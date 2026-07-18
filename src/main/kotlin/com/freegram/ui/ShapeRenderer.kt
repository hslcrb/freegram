package com.freegram.ui

import com.freegram.model.*
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.math.*

object ShapeRenderer {

    fun drawShape(gc: GraphicsContext, shapeType: String, x: Double, y: Double, w: Double, h: Double, style: Style) {
        gc.save()
        gc.translate(x + w / 2, y + h / 2)
        gc.rotate(Math.toRadians(style.rotation))
        gc.translate(-(x + w / 2), -(y + h / 2))

        when (shapeType) {
            ShapeTypes.RECTANGLE -> drawRect(gc, x, y, w, h)
            ShapeTypes.ROUND_RECT -> drawRoundRect(gc, x, y, w, h, style.cornerRadius)
            ShapeTypes.CIRCLE -> drawCircle(gc, x, y, w, h)
            ShapeTypes.OVAL -> drawOval(gc, x, y, w, h)
            ShapeTypes.DIAMOND -> drawDiamond(gc, x, y, w, h)
            ShapeTypes.STADIUM -> drawStadium(gc, x, y, w, h)
            ShapeTypes.PARALLELOGRAM -> drawParallelogram(gc, x, y, w, h)
            ShapeTypes.PARALLELOGRAM_ALT -> drawParallelogramAlt(gc, x, y, w, h)
            ShapeTypes.TRAPEZOID -> drawTrapezoid(gc, x, y, w, h)
            ShapeTypes.TRAPEZOID_ALT -> drawTrapezoidAlt(gc, x, y, w, h)
            ShapeTypes.HEXAGON -> drawHexagon(gc, x, y, w, h)
            ShapeTypes.PENTAGON -> drawPentagon(gc, x, y, w, h)
            ShapeTypes.OCTAGON -> drawOctagon(gc, x, y, w, h)
            ShapeTypes.STAR -> drawStar(gc, x, y, w, h, 5)
            ShapeTypes.STAR_4 -> drawStar(gc, x, y, w, h, 4)
            ShapeTypes.STAR_6 -> drawStar(gc, x, y, w, h, 6)
            ShapeTypes.ARROW_UP -> drawArrowHead(gc, x, y, w, h, -PI / 2)
            ShapeTypes.ARROW_DOWN -> drawArrowHead(gc, x, y, w, h, PI / 2)
            ShapeTypes.ARROW_LEFT -> drawArrowHead(gc, x, y, w, h, PI)
            ShapeTypes.ARROW_RIGHT -> drawArrowHead(gc, x, y, w, h, 0.0)
            ShapeTypes.TRIANGLE -> drawTriangle(gc, x, y, w, h, true)
            ShapeTypes.TRIANGLE_DOWN -> drawTriangle(gc, x, y, w, h, false)
            ShapeTypes.CROSS -> drawCross(gc, x, y, w, h)
            ShapeTypes.HEART -> drawHeart(gc, x, y, w, h)
            ShapeTypes.CLOUD -> drawCloud(gc, x, y, w, h)
            ShapeTypes.CYLINDER -> drawCylinder(gc, x, y, w, h)
            ShapeTypes.DOCUMENT -> drawDocument(gc, x, y, w, h)
            ShapeTypes.NOTE -> drawNote(gc, x, y, w, h)
            ShapeTypes.PROCESS -> drawProcess(gc, x, y, w, h)
            ShapeTypes.DELAY -> drawDelay(gc, x, y, w, h)
            ShapeTypes.MANUAL_INPUT -> drawManualInput(gc, x, y, w, h)
            ShapeTypes.CARD -> drawCard(gc, x, y, w, h)
            ShapeTypes.OR -> drawOr(gc, x, y, w, h)
            else -> drawRect(gc, x, y, w, h)
        }

        gc.restore()
    }

    private fun applyFillStroke(gc: GraphicsContext, style: Style, drawFill: Boolean = true) {
        if (drawFill && style.fillColor.isNotEmpty() && style.opacity > 0) {
            gc.fill = Color.web(style.fillColor, style.opacity)
        }
        gc.stroke = Color.web(style.strokeColor, style.opacity)
        gc.lineWidth = style.strokeWidth
        when (style.strokeDash) {
            "dashed" -> gc.setLineDashes(8.0, 4.0)
            "dotted" -> gc.setLineDashes(2.0, 4.0)
            "dashdot" -> gc.setLineDashes(8.0, 4.0, 2.0, 4.0)
            else -> gc.setLineDashes()
        }
    }

    private fun drawRect(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        gc.fillRect(x, y, w, h)
        gc.strokeRect(x, y, w, h)
    }

    private fun drawRoundRect(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double, r: Double) {
        val cr = minOf(r, minOf(w, h) / 2)
        gc.fillRoundRect(x, y, w, h, cr * 2, cr * 2)
        gc.strokeRoundRect(x, y, w, h, cr * 2, cr * 2)
    }

    private fun drawCircle(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val r = minOf(w, h) / 2
        gc.fillOval(x + w / 2 - r, y + h / 2 - r, r * 2, r * 2)
        gc.strokeOval(x + w / 2 - r, y + h / 2 - r, r * 2, r * 2)
    }

    private fun drawOval(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        gc.fillOval(x, y, w, h)
        gc.strokeOval(x, y, w, h)
    }

    private fun drawDiamond(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        gc.beginPath()
        gc.moveTo(x + w / 2, y)
        gc.lineTo(x + w, y + h / 2)
        gc.lineTo(x + w / 2, y + h)
        gc.lineTo(x, y + h / 2)
        gc.closePath()
        gc.fill()
        gc.stroke()
    }

    private fun drawStadium(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val arc = minOf(w, h) / 2
        gc.fillRoundRect(x, y, w, h, arc * 2, arc * 2)
        gc.strokeRoundRect(x, y, w, h, arc * 2, arc * 2)
    }

    private fun drawParallelogram(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val skew = w * 0.2
        gc.beginPath()
        gc.moveTo(x + skew, y)
        gc.lineTo(x + w, y)
        gc.lineTo(x + w - skew, y + h)
        gc.lineTo(x, y + h)
        gc.closePath()
        gc.fill(); gc.stroke()
    }

    private fun drawParallelogramAlt(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val skew = w * 0.2
        gc.beginPath()
        gc.moveTo(x, y)
        gc.lineTo(x + w - skew, y)
        gc.lineTo(x + w, y + h)
        gc.lineTo(x + skew, y + h)
        gc.closePath()
        gc.fill(); gc.stroke()
    }

    private fun drawTrapezoid(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val inset = w * 0.2
        gc.beginPath()
        gc.moveTo(x + inset, y)
        gc.lineTo(x + w - inset, y)
        gc.lineTo(x + w, y + h)
        gc.lineTo(x, y + h)
        gc.closePath()
        gc.fill(); gc.stroke()
    }

    private fun drawTrapezoidAlt(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val inset = w * 0.2
        gc.beginPath()
        gc.moveTo(x, y)
        gc.lineTo(x + w, y)
        gc.lineTo(x + w - inset, y + h)
        gc.lineTo(x + inset, y + h)
        gc.closePath()
        gc.fill(); gc.stroke()
    }

    private fun drawHexagon(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        gc.beginPath()
        for (i in 0 until 6) {
            val angle = PI / 3 * i - PI / 6
            val px = x + w / 2 + w / 2 * cos(angle)
            val py = y + h / 2 + h / 2 * sin(angle)
            if (i == 0) gc.moveTo(px, py) else gc.lineTo(px, py)
        }
        gc.closePath(); gc.fill(); gc.stroke()
    }

    private fun drawPentagon(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        gc.beginPath()
        for (i in 0 until 5) {
            val angle = 2 * PI / 5 * i - PI / 2
            val px = x + w / 2 + w / 2 * cos(angle)
            val py = y + h / 2 + h / 2 * sin(angle)
            if (i == 0) gc.moveTo(px, py) else gc.lineTo(px, py)
        }
        gc.closePath(); gc.fill(); gc.stroke()
    }

    private fun drawOctagon(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val cx = x + w / 2; val cy = y + h / 2
        val rx = w / 2; val ry = h / 2
        gc.beginPath()
        for (i in 0 until 8) {
            val angle = PI / 4 * i + PI / 8
            val px = cx + rx * cos(angle)
            val py = cy + ry * sin(angle)
            if (i == 0) gc.moveTo(px, py) else gc.lineTo(px, py)
        }
        gc.closePath(); gc.fill(); gc.stroke()
    }

    private fun drawStar(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double, points: Int) {
        val cx = x + w / 2; val cy = y + h / 2
        val outerR = minOf(w, h) / 2.0
        val innerR = outerR * 0.4
        gc.beginPath()
        for (i in 0 until points * 2) {
            val angle = PI / points * i - PI / 2
            val r = if (i % 2 == 0) outerR else innerR
            val px = cx + r * cos(angle)
            val py = cy + r * sin(angle)
            if (i == 0) gc.moveTo(px, py) else gc.lineTo(px, py)
        }
        gc.closePath(); gc.fill(); gc.stroke()
    }

    private fun drawArrowHead(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double, angle: Double) {
        val cx = x + w / 2; val cy = y + h / 2
        val len = minOf(w, h) * 0.8
        gc.beginPath()
        gc.moveTo(cx + len / 2 * cos(angle), cy + len / 2 * sin(angle))
        gc.lineTo(cx + len / 2 * cos(angle + 2.5), cy + len / 2 * sin(angle + 2.5))
        gc.lineTo(cx + len / 2 * cos(angle - 2.5), cy + len / 2 * sin(angle - 2.5))
        gc.closePath(); gc.fill(); gc.stroke()
    }

    private fun drawTriangle(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double, up: Boolean) {
        val cx = x + w / 2
        val topY = if (up) y else y + h
        val botY = if (up) y + h else y
        gc.beginPath()
        gc.moveTo(cx, topY)
        gc.lineTo(x + w, botY)
        gc.lineTo(x, botY)
        gc.closePath(); gc.fill(); gc.stroke()
    }

    private fun drawCross(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val cx = x + w / 2; val cy = y + h / 2
        val arm = minOf(w, h) * 0.3
        gc.beginPath()
        val pts = arrayOf(
            cx - arm to cy - arm, cx - arm / 3 to cy - arm, cx - arm / 3 to cy - arm / 3,
            cx - arm to cy - arm / 3, cx - arm to cy + arm / 3, cx - arm / 3 to cy + arm / 3,
            cx - arm / 3 to cy + arm, cx + arm / 3 to cy + arm, cx + arm / 3 to cy + arm / 3,
            cx + arm to cy + arm / 3, cx + arm to cy - arm / 3, cx + arm / 3 to cy - arm / 3,
            cx + arm / 3 to cy - arm, cx - arm / 3 to cy - arm
        )
        pts.forEachIndexed { i, (px, py) ->
            if (i == 0) gc.moveTo(px, py) else gc.lineTo(px, py)
        }
        gc.closePath(); gc.fill(); gc.stroke()
    }

    private fun drawHeart(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val cx = x + w / 2; val cy = y + h / 2
        val s = minOf(w, h) / 2.0
        gc.beginPath()
        gc.moveTo(cx, cy + s * 0.7)
        gc.bezierCurveTo(cx - s, cy - s * 0.3, cx - s * 0.5, cy - s * 0.9, cx, cy - s * 0.3)
        gc.bezierCurveTo(cx + s * 0.5, cy - s * 0.9, cx + s, cy - s * 0.3, cx, cy + s * 0.7)
        gc.fill(); gc.stroke()
    }

    private fun drawCloud(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val cx = x + w / 2; val cy = y + h / 2
        val r = minOf(w, h) / 4
        gc.beginPath()
        gc.moveTo(cx + r * 2, cy + r * 0.5)
        gc.bezierCurveTo(cx + r * 3, cy - r, cx + r * 0.5, cy - r * 1.5, cx, cy - r * 0.5)
        gc.bezierCurveTo(cx - r * 1.5, cy - r * 2, cx - r * 3, cy, cx - r * 2, cy + r)
        gc.bezierCurveTo(cx - r * 2.5, cy + r * 2, cx - r, cy + r * 1.5, cx - r * 0.5, cy + r * 1.5)
        gc.bezierCurveTo(cx - r * 0.5, cy + r * 2.5, cx + r, cy + r * 2.5, cx + r * 1.5, cy + r * 1.5)
        gc.bezierCurveTo(cx + r * 2.5, cy + r * 1.5, cx + r * 2.5, cy + r * 0.5, cx + r * 2, cy + r * 0.5)
        gc.fill(); gc.stroke()
    }

    private fun drawCylinder(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val ellipseH = h * 0.2
        gc.fillOval(x, y, w, ellipseH)
        gc.fillRect(x, y + ellipseH / 2, w, h - ellipseH)
        gc.fillOval(x, y + h - ellipseH, w, ellipseH)
        gc.strokeOval(x, y, w, ellipseH)
        gc.strokeLine(x, y + ellipseH / 2, x, y + h - ellipseH / 2)
        gc.strokeLine(x + w, y + ellipseH / 2, x + w, y + h - ellipseH / 2)
        gc.strokeOval(x, y + h - ellipseH, w, ellipseH)
    }

    private fun drawDocument(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val fold = w * 0.2
        gc.beginPath()
        gc.moveTo(x, y); gc.lineTo(x + w - fold, y)
        gc.lineTo(x + w, y + fold); gc.lineTo(x + w, y + h)
        gc.lineTo(x, y + h); gc.closePath()
        gc.fill()
        gc.strokeLine(x + w - fold, y, x + w - fold, y + fold)
        gc.strokeLine(x + w - fold, y + fold, x + w, y + fold)
        gc.strokeRect(x, y, w, h)
    }

    private fun drawNote(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val fold = w * 0.15
        gc.beginPath()
        gc.moveTo(x, y); gc.lineTo(x + w - fold, y)
        gc.lineTo(x + w, y + fold); gc.lineTo(x + w, y + h)
        gc.lineTo(x, y + h); gc.closePath()
        gc.fill()
        gc.strokeLine(x + w - fold, y, x + w - fold, y + fold)
        gc.strokeLine(x + w - fold, y + fold, x + w, y + fold)
        gc.strokeRect(x, y, w, h)
    }

    private fun drawProcess(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val inset = w * 0.05
        gc.fillRect(x + inset, y, w - inset * 2, h)
        gc.fillRect(x, y + inset, w, h - inset * 2)
        gc.strokeRect(x, y, w, h)
    }

    private fun drawDelay(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val cx = x + w / 2; val cy = y + h / 2
        val r = minOf(w, h) / 2
        gc.beginPath()
        gc.arc(cx, cy, r, r, 0.0, -180.0)
        gc.closePath()
        gc.fill()
        gc.stroke()
    }

    private fun drawManualInput(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        gc.beginPath()
        gc.moveTo(x + w * 0.3, y)
        gc.lineTo(x + w, y)
        gc.lineTo(x + w * 0.7, y + h)
        gc.lineTo(x, y + h)
        gc.closePath(); gc.fill(); gc.stroke()
    }

    private fun drawCard(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val r = minOf(w, h) * 0.08
        gc.fillRoundRect(x, y, w, h, r * 2, r * 2)
        gc.strokeRoundRect(x, y, w, h, r * 2, r * 2)
    }

    private fun drawOr(gc: GraphicsContext, x: Double, y: Double, w: Double, h: Double) {
        val cx = x + w / 2; val cy = y + h / 2
        val r = minOf(w, h) / 2
        gc.beginPath()
        gc.arc(cx, cy, r, r, 90.0, -180.0)
        gc.closePath(); gc.fill(); gc.stroke()
    }
}
