package com.freegram.ui

import com.freegram.model.*
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.transform.Affine
import kotlin.math.*

class DiagramCanvas : Canvas() {

    private var document: FdmlDocument = FdmlDocument()
    private var selectedElementId: String? = null
    private var isDragging = false
    private var dragStartX = 0.0
    private var dragStartY = 0.0
    private var isPanning = false
    private var panStartX = 0.0
    private var panStartY = 0.0
    private var zoomLevel = 1.0
    private var panOffsetX = 0.0
    private var panOffsetY = 0.0
    private var clipboard: List<DiagramElement> = emptyList()
    private var isCreatingEdge = false
    private var edgeSourceId: String? = null
    private var mouseX = 0.0
    private var mouseY = 0.0

    var onSelectionChanged: ((DiagramElement?) -> Unit)? = null
    var onDocumentModified: (() -> Unit)? = null

    init {
        width = 1200.0
        height = 900.0

        setOnScroll { e ->
            val delta = -e.deltaY * 0.001
            zoomLevel = (zoomLevel * (1 + delta)).coerceIn(0.1, 10.0)
            render()
        }

        setOnMousePressed { e ->
            if (e.isMiddleButtonDown || e.isControlDown) {
                isPanning = true
                panStartX = e.x
                panStartY = e.y
                return@setOnMousePressed
            }
            isDragging = true
            dragStartX = e.x
            dragStartY = e.y
            mouseX = e.x
            mouseY = e.y
            val wx = screenToWorldX(e.x)
            val wy = screenToWorldY(e.y)
            handleClick(wx, wy, e)
        }

        setOnMouseReleased {
            isDragging = false
            isPanning = false
            if (isCreatingEdge) { isCreatingEdge = false; edgeSourceId = null }
        }

        setOnMouseDragged { e ->
            mouseX = e.x; mouseY = e.y
            if (isPanning) {
                panOffsetX += e.x - panStartX
                panOffsetY += e.y - panStartY
                panStartX = e.x; panStartY = e.y
                render(); return@setOnMouseDragged
            }
            if (isDragging && selectedElementId != null) {
                val dx = (e.x - dragStartX) / zoomLevel
                val dy = (e.y - dragStartY) / zoomLevel
                dragStartX = e.x; dragStartY = e.y
                if (document.snapToGrid) {
                    moveSelectedElementSnapped(dx, dy)
                } else {
                    moveSelectedElement(dx, dy)
                }
            }
        }

        setOnKeyPressed { e ->
            when (e.code) {
                KeyCode.DELETE, KeyCode.BACK_SPACE -> deleteSelected()
                KeyCode.C -> if (e.isControlDown) copySelected()
                KeyCode.V -> if (e.isControlDown) pasteClipboard()
                KeyCode.X -> if (e.isControlDown) { copySelected(); deleteSelected() }
                KeyCode.Z -> if (e.isControlDown) {
                    if (e.isShiftDown) redoAction?.invoke() else undoAction?.invoke()
                }
                KeyCode.EQUALS, KeyCode.PLUS -> if (e.isControlDown) zoomIn()
                KeyCode.MINUS -> if (e.isControlDown) zoomOut()
                KeyCode.DIGIT0 -> if (e.isControlDown) zoomReset()
                else -> {}
            }
        }
    }

    var undoAction: (() -> Unit)? = null
    var redoAction: (() -> Unit)? = null

    fun setDocument(doc: FdmlDocument) {
        this.document = doc
        width = doc.diagramWidth
        height = doc.diagramHeight
        zoomLevel = doc.zoomLevel
        render()
        requestFocus()
    }

    fun getDocument(): FdmlDocument = document
    fun getSelectedId(): String? = selectedElementId

    fun render() {
        val gc = graphicsContext2D
        gc.clearRect(0.0, 0.0, width, height)
        gc.save()

        gc.translate(panOffsetX, panOffsetY)
        gc.scale(zoomLevel, zoomLevel)

        drawBackground(gc)
        if (document.gridEnabled) drawGrid(gc)
        if (document.showRulers) drawRulers(gc)

        val visibleElements = document.getVisibleElements()
        visibleElements.forEach { element ->
            drawElementEffects(gc, element)
        }
        visibleElements.forEach { element ->
            drawElement(gc, element)
        }

        gc.restore()
    }

    fun requestCanvasFocus() { requestFocus() }

    private fun drawBackground(gc: GraphicsContext) {
        gc.fill = Color.web(document.backgroundColor)
        gc.fillRect(-50.0, -50.0, width + 100, height + 100)
    }

    private fun drawGrid(gc: GraphicsContext) {
        gc.stroke = Color.rgb(200, 200, 200, 0.3)
        gc.lineWidth = 0.5

        val gs = document.gridSize
        var x = 0.0
        while (x < width) {
            gc.strokeLine(x, 0.0, x, height)
            x += gs
        }
        var y = 0.0
        while (y < height) {
            gc.strokeLine(0.0, y, width, y)
            y += gs
        }
    }

    private fun drawRulers(gc: GraphicsContext) {
        val rulerSize = 15.0
        gc.fill = Color.rgb(240, 240, 240, 0.8)
        gc.fillRect(0.0, 0.0, width, rulerSize)
        gc.fillRect(0.0, 0.0, rulerSize, height)
        gc.stroke = Color.rgb(180, 180, 180)
        gc.lineWidth = 0.5
        gc.strokeLine(0.0, rulerSize, width, rulerSize)
        gc.strokeLine(rulerSize, 0.0, rulerSize, height)

        gc.fill = Color.rgb(100, 100, 100)
        gc.font = Font.font("sans-serif", 8.0)
        val step = 100.0
        var x = step
        while (x < width) {
            gc.strokeLine(x, 0.0, x, rulerSize)
            gc.fillText("%.0f".format(x), x + 2.0, rulerSize - 2.0)
            x += step
        }
        var y = step
        while (y < height) {
            gc.strokeLine(0.0, y, rulerSize, y)
            gc.fillText("%.0f".format(y), 2.0, y + 8.0)
            y += step
        }
    }

    private fun drawElementEffects(gc: GraphicsContext, element: DiagramElement) {
        val s = element.style
        val bounds = getElementBounds(element)

        if (s.glow.enabled) {
            gc.save()
            gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.SRC_OVER)
            val glowColor = Color.web(s.glow.color, s.glow.opacity * 0.3)
            gc.fill = glowColor
            val g = s.glow.radius
            gc.fillOval(bounds.x - g, bounds.y - g, bounds.width + g * 2, bounds.height + g * 2)
            gc.restore()
        }

        if (s.dropShadow.enabled) {
            gc.save()
            val shadowColor = Color.web(s.dropShadow.color, s.dropShadow.opacity)
            gc.fill = shadowColor
            val sx = bounds.x + s.dropShadow.offsetX
            val sy = bounds.y + s.dropShadow.offsetY
            ShapeRenderer.drawShape(gc, s.shapeType, sx, sy, bounds.width, bounds.height, s)
            gc.restore()
        }
    }

    private fun getElementBounds(element: DiagramElement): Bounds {
        return when (element) {
            is DiagramElement.Node -> Bounds(element.position.x, element.position.y, element.size.width, element.size.height)
            is DiagramElement.Text -> Bounds(element.position.x, element.position.y - element.fontSize, element.text.length * element.fontSize * 0.6, element.fontSize * 1.2)
            is DiagramElement.MermaidBlock -> Bounds(element.position.x, element.position.y, element.width, element.height)
            is DiagramElement.PdfElement -> Bounds(element.position.x, element.position.y, element.width, element.height)
            is DiagramElement.Edge -> Bounds(element.position.x - 10, element.position.y - 10, 20.0, 20.0)
            is DiagramElement.Group -> Bounds(element.position.x, element.position.y, 100.0, 50.0)
        }
    }

    private fun drawElement(gc: GraphicsContext, element: DiagramElement) {
        when (element) {
            is DiagramElement.Node -> drawNode(gc, element)
            is DiagramElement.Edge -> drawEdge(gc, element)
            is DiagramElement.Text -> drawTextElement(gc, element)
            is DiagramElement.MermaidBlock -> drawMermaidBlock(gc, element)
            is DiagramElement.PdfElement -> drawPdfElement(gc, element)
            is DiagramElement.Group -> drawGroup(gc, element)
        }
    }

    private fun drawNode(gc: GraphicsContext, node: DiagramElement.Node) {
        val x = node.position.x; val y = node.position.y
        val w = node.size.width; val h = node.size.height
        applyStyle(gc, node.style)
        ShapeRenderer.drawShape(gc, node.style.shapeType, x, y, w, h, node.style)

        if (node.content.isNotEmpty()) {
            gc.fill = Color.web(node.style.fontColor)
            gc.font = Font.font(node.style.fontFamily, FontWeight.NORMAL, node.style.fontSize)
            val textX = x + w / 2 - computeTextWidth(node.content, node.style.fontSize) / 2
            val textY = y + h / 2 + node.style.fontSize / 3
            gc.fillText(node.content, textX, textY)
        }

        if (node.id == selectedElementId) {
            gc.stroke = Color.BLUE
            gc.lineWidth = 2.0 / zoomLevel
            gc.setLineDashes(4.0 / zoomLevel, 4.0 / zoomLevel)
            gc.strokeRect(x - 3, y - 3, w + 6, h + 6)
            gc.setLineDashes()
        }

        if (node.style.tooltip.isNotEmpty()) {
            gc.fill = Color.rgb(255, 255, 200, 0.9)
            gc.fill = Color.rgb(100, 100, 100)
            gc.font = Font.font("sans-serif", 9.0)
        }
    }

    private fun drawEdge(gc: GraphicsContext, edge: DiagramElement.Edge) {
        val deco = edge.style.edgeDecoration
        applyStyle(gc, edge.style)
        gc.lineWidth = edge.style.strokeWidth

        when (deco.dashPattern) {
            EdgeDashPattern.DASHED -> gc.setLineDashes(8.0, 4.0)
            EdgeDashPattern.DOTTED -> gc.setLineDashes(2.0, 4.0)
            EdgeDashPattern.DASHDOT -> gc.setLineDashes(8.0, 4.0, 2.0, 4.0)
            EdgeDashPattern.LONGDASH -> gc.setLineDashes(16.0, 6.0)
            else -> gc.setLineDashes()
        }

        if (edge.waypoints.size >= 2) {
            gc.beginPath()
            val pts = edge.waypoints.map { it.position }
            gc.moveTo(pts[0].x, pts[0].y)
            if (deco.curved && pts.size >= 3) {
                for (i in 1 until pts.size - 1) {
                    val midX = (pts[i].x + pts[i + 1].x) / 2
                    val midY = (pts[i].y + pts[i + 1].y) / 2
                    gc.quadraticCurveTo(pts[i].x, pts[i].y, midX, midY)
                }
                gc.lineTo(pts.last().x, pts.last().y)
            } else {
                for (i in 1 until pts.size) {
                    gc.lineTo(pts[i].x, pts[i].y)
                }
            }
            gc.stroke()

            val last = pts.last(); val prev = if (pts.size >= 2) pts[pts.size - 2] else last
            drawArrowhead(gc, prev.x, prev.y, last.x, last.y, deco.targetArrow, edge.style.strokeColor)
            if (deco.sourceArrow != EdgeArrowType.NONE) {
                val first = pts.first(); val second = if (pts.size >= 2) pts[1] else first
                drawArrowhead(gc, second.x, second.y, first.x, first.y, deco.sourceArrow, edge.style.strokeColor)
            }
        }

        gc.setLineDashes()

        if (edge.label.isNotEmpty()) {
            gc.fill = Color.web(edge.style.fontColor)
            gc.font = Font.font(edge.style.fontFamily, 10.0)
            gc.fillText(edge.label, edge.position.x + 10, edge.position.y + 10)
        }
    }

    private fun drawArrowhead(gc: GraphicsContext, fromX: Double, fromY: Double, toX: Double, toY: Double, type: EdgeArrowType, color: String) {
        if (type == EdgeArrowType.NONE) return
        val angle = atan2(toY - fromY, toX - fromX)
        val len = when (type) {
            EdgeArrowType.TRIANGLE -> 12.0; EdgeArrowType.DIAMOND -> 10.0
            EdgeArrowType.CIRCLE -> 6.0; EdgeArrowType.OPEN -> 10.0
            else -> 8.0
        }
        gc.fill = Color.web(color)
        gc.stroke = Color.web(color)

        when (type) {
            EdgeArrowType.TRIANGLE -> {
                gc.beginPath()
                gc.moveTo(toX, toY)
                gc.lineTo(toX - len * cos(angle - 0.5), toY - len * sin(angle - 0.5))
                gc.lineTo(toX - len * cos(angle + 0.5), toY - len * sin(angle + 0.5))
                gc.closePath(); gc.fill()
            }
            EdgeArrowType.OPEN -> {
                gc.lineWidth = 1.5
                gc.strokeLine(toX, toY, toX - len * cos(angle - 0.5), toY - len * sin(angle - 0.5))
                gc.strokeLine(toX, toY, toX - len * cos(angle + 0.5), toY - len * sin(angle + 0.5))
            }
            EdgeArrowType.DIAMOND -> {
                val hw = len / 2; val hl = len
                gc.beginPath()
                gc.moveTo(toX, toY)
                gc.lineTo(toX - hl / 2 * cos(angle) - hw * cos(angle + PI / 2), toY - hl / 2 * sin(angle) - hw * sin(angle + PI / 2))
                gc.lineTo(toX - hl * cos(angle), toY - hl * sin(angle))
                gc.lineTo(toX - hl / 2 * cos(angle) + hw * cos(angle + PI / 2), toY - hl / 2 * sin(angle) + hw * sin(angle + PI / 2))
                gc.closePath(); gc.fill()
            }
            EdgeArrowType.CIRCLE -> {
                gc.fillOval(toX - len / 2, toY - len / 2, len, len)
            }
            else -> {
                gc.strokeLine(toX, toY, toX - len * cos(angle - 0.5), toY - len * sin(angle - 0.5))
                gc.strokeLine(toX, toY, toX - len * cos(angle + 0.5), toY - len * sin(angle + 0.5))
            }
        }
    }

    private fun drawTextElement(gc: GraphicsContext, text: DiagramElement.Text) {
        gc.fill = Color.web(text.style.fontColor, text.style.opacity)
        gc.font = Font.font(text.style.fontFamily, FontWeight.NORMAL, text.fontSize)
        gc.fillText(text.text, text.position.x, text.position.y)
    }

    private fun drawMermaidBlock(gc: GraphicsContext, block: DiagramElement.MermaidBlock) {
        val x = block.position.x; val y = block.position.y
        val w = block.width; val h = block.height

        gc.fill = Color.rgb(200, 200, 255, 0.3)
        gc.fillRect(x, y, w, h)
        gc.stroke = Color.rgb(100, 100, 200)
        gc.lineWidth = 1.0
        gc.strokeRect(x, y, w, h)
        gc.fill = Color.rgb(100, 100, 150)
        gc.font = Font.font("monospace", 12.0)
        val label = if (block.isNativeMermaid) "[Mermaid: ${block.id}]" else "[Extended FDML: ${block.id}]"
        gc.fillText(label, x + 4, y + 16)
        if (block.isNativeMermaid) {
            val lines = block.mermaidCode.lines().take(5)
            gc.font = Font.font("monospace", 10.0)
            var ly = y + 36
            for (line in lines) {
                gc.fillText(line.take(40), x + 4, ly)
                ly += 14
            }
        }
    }

    private fun drawPdfElement(gc: GraphicsContext, pdf: DiagramElement.PdfElement) {
        val x = pdf.position.x; val y = pdf.position.y; val w = pdf.width; val h = pdf.height
        gc.fill = Color.rgb(240, 240, 240)
        gc.fillRect(x, y, w, h)
        gc.stroke = Color.rgb(150, 150, 150)
        gc.lineWidth = 1.0
        gc.strokeRect(x, y, w, h)
        gc.fill = Color.rgb(100, 100, 100)
        gc.font = Font.font("sans-serif", 11.0)
        gc.fillText("[PDF: ${pdf.id} page ${pdf.pageNumber}]", x + 4, y + h / 2)
    }

    private fun drawGroup(gc: GraphicsContext, group: DiagramElement.Group) {
        gc.stroke = Color.rgb(0, 0, 0, 0.2)
        gc.lineWidth = 1.0
        val x = group.position.x; val y = group.position.y
        gc.strokeRect(x, y, 100.0, 50.0)
        gc.fill = Color.rgb(0, 0, 0, 0.3)
        gc.font = Font.font("sans-serif", 10.0)
        gc.fillText("[Group: ${group.id}]", x + 4, y + 14)
        group.elements.forEach { drawElement(gc, it) }
    }

    private fun applyStyle(gc: GraphicsContext, style: Style) {
        gc.fill = Color.web(style.fillColor, style.opacity)
        gc.stroke = Color.web(style.strokeColor, style.opacity)
        gc.lineWidth = style.strokeWidth
    }

    private fun computeTextWidth(text: String, fontSize: Double): Double {
        return text.length * fontSize * 0.55
    }

    private fun screenToWorldX(sx: Double): Double = (sx - panOffsetX) / zoomLevel
    private fun screenToWorldY(sy: Double): Double = (sy - panOffsetY) / zoomLevel

    private fun handleClick(wx: Double, wy: Double, event: MouseEvent) {
        selectedElementId = null
        for (element in document.getVisibleElements().reversed()) {
            if (hitTest(element, wx, wy)) {
                selectedElementId = element.id
                if (event.clickCount >= 2 && element is DiagramElement.Node) {
                    startEdgeCreation(element.id)
                }
                break
            }
        }
        onSelectionChanged?.invoke(document.elements.find { it.id == selectedElementId })
        render()
    }

    private fun startEdgeCreation(nodeId: String) {
        isCreatingEdge = true
        edgeSourceId = nodeId
    }

    private fun hitTest(element: DiagramElement, x: Double, y: Double): Boolean {
        return when (element) {
            is DiagramElement.Node -> x >= element.position.x && x <= element.position.x + element.size.width &&
                y >= element.position.y && y <= element.position.y + element.size.height
            is DiagramElement.MermaidBlock -> x >= element.position.x && x <= element.position.x + element.width &&
                y >= element.position.y && y <= element.position.y + element.height
            is DiagramElement.Text -> {
                val approxWidth = element.text.length * element.fontSize * 0.6
                x >= element.position.x && x <= element.position.x + approxWidth &&
                    y >= element.position.y - element.fontSize && y <= element.position.y
            }
            is DiagramElement.PdfElement -> x >= element.position.x && x <= element.position.x + element.width &&
                y >= element.position.y && y <= element.position.y + element.height
            is DiagramElement.Edge -> false
            is DiagramElement.Group -> element.elements.any { hitTest(it, x, y) }
        }
    }

    private fun moveSelectedElement(dx: Double, dy: Double) {
        val element = document.elements.find { it.id == selectedElementId } ?: return
        val idx = document.elements.indexOf(element)
        document.elements[idx] = when (element) {
            is DiagramElement.Node -> element.copy(position = Position(element.position.x + dx, element.position.y + dy))
            is DiagramElement.Edge -> element.copy(
                position = Position(element.position.x + dx, element.position.y + dy),
                waypoints = element.waypoints.map { wp -> DiagramElement.EdgeWaypoint(Position(wp.position.x + dx, wp.position.y + dy)) }
            )
            is DiagramElement.Text -> element.copy(position = Position(element.position.x + dx, element.position.y + dy))
            is DiagramElement.MermaidBlock -> element.copy(position = Position(element.position.x + dx, element.position.y + dy))
            is DiagramElement.PdfElement -> element.copy(position = Position(element.position.x + dx, element.position.y + dy))
            is DiagramElement.Group -> element.copy(position = Position(element.position.x + dx, element.position.y + dy))
        }
        render()
        onDocumentModified?.invoke()
    }

    private fun moveSelectedElementSnapped(dx: Double, dy: Double) {
        val gs = document.gridSize
        val snapX = round((document.elements.find { it.id == selectedElementId }?.position?.x ?: 0.0 + dx) / gs) * gs
        val snapY = round((document.elements.find { it.id == selectedElementId }?.position?.y ?: 0.0 + dy) / gs) * gs
        val element = document.elements.find { it.id == selectedElementId } ?: return
        val idx = document.elements.indexOf(element)
        val newX = snapX
        val newY = snapY
        document.elements[idx] = when (element) {
            is DiagramElement.Node -> element.copy(position = Position(newX, newY))
            is DiagramElement.Edge -> element.copy(position = Position(newX, newY))
            is DiagramElement.Text -> element.copy(position = Position(newX, newY))
            is DiagramElement.MermaidBlock -> element.copy(position = Position(newX, newY))
            is DiagramElement.PdfElement -> element.copy(position = Position(newX, newY))
            is DiagramElement.Group -> element.copy(position = Position(newX, newY))
        }
        render()
    }

    fun deleteSelected() {
        val id = selectedElementId ?: return
        document.elements.removeAll { it.id == id }
        selectedElementId = null
        render()
        onSelectionChanged?.invoke(null)
        onDocumentModified?.invoke()
    }

    fun copySelected() {
        val id = selectedElementId ?: return
        val el = document.elements.find { it.id == id } ?: return
        clipboard = listOf(el)
    }

    fun pasteClipboard() {
        if (clipboard.isEmpty()) return
        clipboard.forEach { el ->
            val copy = when (el) {
                is DiagramElement.Node -> el.copy(id = "copy_${el.id}", position = Position(el.position.x + 20, el.position.y + 20))
                is DiagramElement.Edge -> el.copy(id = "copy_${el.id}", position = Position(el.position.x + 20, el.position.y + 20))
                is DiagramElement.Text -> el.copy(id = "copy_${el.id}", position = Position(el.position.x + 20, el.position.y + 20))
                is DiagramElement.MermaidBlock -> el.copy(id = "copy_${el.id}", position = Position(el.position.x + 20, el.position.y + 20))
                is DiagramElement.PdfElement -> el.copy(id = "copy_${el.id}", position = Position(el.position.x + 20, el.position.y + 20))
                is DiagramElement.Group -> el.copy(id = "copy_${el.id}", position = Position(el.position.x + 20, el.position.y + 20))
            }
            document.elements.add(copy)
        }
        render()
        onDocumentModified?.invoke()
    }

    fun zoomIn() { zoomLevel = (zoomLevel * 1.2).coerceAtMost(10.0); render() }
    fun zoomOut() { zoomLevel = (zoomLevel / 1.2).coerceAtLeast(0.1); render() }
    fun zoomReset() { zoomLevel = 1.0; panOffsetX = 0.0; panOffsetY = 0.0; render() }
    fun zoomToFit() {
        if (document.elements.isEmpty()) return
        val allBounds = document.elements.map { getElementBounds(it) }
        val minX = allBounds.minOf { it.x }; val minY = allBounds.minOf { it.y }
        val maxX = allBounds.maxOf { it.x + it.width }; val maxY = allBounds.maxOf { it.y + it.height }
        val margin = 50.0
        val scaleX = (width - margin * 2) / (maxX - minX + margin * 2)
        val scaleY = (height - margin * 2) / (maxY - minY + margin * 2)
        zoomLevel = minOf(scaleX, scaleY).coerceIn(0.1, 5.0)
        panOffsetX = -(minX - margin) * zoomLevel + (width - (maxX - minX + margin * 2) * zoomLevel) / 2
        panOffsetY = -(minY - margin) * zoomLevel + (height - (maxY - minY + margin * 2) * zoomLevel) / 2
        render()
    }
}
