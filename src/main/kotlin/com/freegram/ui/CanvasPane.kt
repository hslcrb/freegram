package com.freegram.ui

import com.freegram.model.*
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

class DiagramCanvas : Canvas() {

    private var document: FdmlDocument = FdmlDocument()
    private val gridSize = 20.0
    private var selectedElementId: String? = null
    private var isDragging = false
    private var dragStartX = 0.0
    private var dragStartY = 0.0

    init {
        width = 1200.0
        height = 900.0

        addEventHandler(MouseEvent.MOUSE_CLICKED) { event ->
            handleClick(event.x, event.y)
        }

        addEventHandler(MouseEvent.MOUSE_PRESSED) { event ->
            isDragging = true
            dragStartX = event.x
            dragStartY = event.y
        }

        addEventHandler(MouseEvent.MOUSE_RELEASED) {
            isDragging = false
        }

        addEventHandler(MouseEvent.MOUSE_DRAGGED) { event ->
            if (isDragging && selectedElementId != null) {
                val dx = event.x - dragStartX
                val dy = event.y - dragStartY
                dragStartX = event.x
                dragStartY = event.y
                moveSelectedElement(dx, dy)
            }
        }
    }

    fun setDocument(doc: FdmlDocument) {
        this.document = doc
        width = doc.diagramWidth
        height = doc.diagramHeight
        render()
    }

    fun getDocument(): FdmlDocument = document

    fun render() {
        val gc = graphicsContext2D
        gc.clearRect(0.0, 0.0, width, height)

        drawBackground(gc)
        if (document.gridEnabled) drawGrid(gc)

        document.elements.forEach { element ->
            drawElement(gc, element)
        }
    }

    private fun drawBackground(gc: GraphicsContext) {
        gc.fill = Color.web(document.backgroundColor)
        gc.fillRect(0.0, 0.0, width, height)
    }

    private fun drawGrid(gc: GraphicsContext) {
        gc.stroke = Color.rgb(200, 200, 200, 0.3)
        gc.lineWidth = 0.5

        var x = 0.0
        while (x < width) {
            gc.strokeLine(x, 0.0, x, height)
            x += document.gridSize
        }
        var y = 0.0
        while (y < height) {
            gc.strokeLine(0.0, y, width, y)
            y += document.gridSize
        }
    }

    private fun drawElement(gc: GraphicsContext, element: DiagramElement) {
        when (element) {
            is DiagramElement.Node -> drawNode(gc, element)
            is DiagramElement.Edge -> drawEdge(gc, element)
            is DiagramElement.Text -> drawText(gc, element)
            is DiagramElement.MermaidBlock -> drawMermaidBlock(gc, element)
            is DiagramElement.PdfElement -> drawPdfElement(gc, element)
            is DiagramElement.Group -> drawGroup(gc, element)
        }
    }

    private fun drawNode(gc: GraphicsContext, node: DiagramElement.Node) {
        val x = node.position.x
        val y = node.position.y
        val w = node.size.width
        val h = node.size.height

        gc.fill = Color.web(node.style.fillColor, node.style.opacity)
        gc.stroke = Color.web(node.style.strokeColor)
        gc.lineWidth = node.style.strokeWidth

        when (node.style.shapeType) {
            "circle" -> {
                val r = minOf(w, h) / 2.0
                gc.fillOval(x + w / 2 - r, y + h / 2 - r, r * 2, r * 2)
                gc.strokeOval(x + w / 2 - r, y + h / 2 - r, r * 2, r * 2)
            }
            "diamond" -> {
                gc.beginPath()
                gc.moveTo(x + w / 2, y)
                gc.lineTo(x + w, y + h / 2)
                gc.lineTo(x + w / 2, y + h)
                gc.lineTo(x, y + h / 2)
                gc.closePath()
                gc.fill()
                gc.stroke()
            }
            "round-rect" -> {
                val arc = minOf(w, h) * 0.2
                gc.fillRoundRect(x, y, w, h, arc, arc)
                gc.strokeRoundRect(x, y, w, h, arc, arc)
            }
            else -> {
                gc.fillRect(x, y, w, h)
                gc.strokeRect(x, y, w, h)
            }
        }

        if (node.content.isNotEmpty()) {
            gc.fill = Color.web(node.style.fontColor)
            gc.font = Font.font(node.style.fontFamily, FontWeight.NORMAL, node.style.fontSize)
            gc.fillText(node.content, x + 4, y + h / 2 + node.style.fontSize / 3)
        }

        if (node.id == selectedElementId) {
            gc.stroke = Color.BLUE
            gc.lineWidth = 2.0
            gc.strokeRect(x - 2, y - 2, w + 4, h + 4)
        }
    }

    private fun drawEdge(gc: GraphicsContext, edge: DiagramElement.Edge) {
        gc.stroke = Color.web(edge.style.strokeColor, edge.style.opacity)
        gc.lineWidth = edge.style.strokeWidth

        if (edge.waypoints.size >= 2) {
            gc.beginPath()
            gc.moveTo(edge.waypoints[0].position.x, edge.waypoints[0].position.y)
            for (i in 1 until edge.waypoints.size) {
                gc.lineTo(edge.waypoints[i].position.x, edge.waypoints[i].position.y)
            }
            gc.stroke()
        }

        if (edge.label.isNotEmpty()) {
            gc.fill = Color.web(edge.style.fontColor)
            gc.font = Font.font(edge.style.fontFamily, 10.0)
            gc.fillText(edge.label, edge.position.x + 10, edge.position.y + 10)
        }
    }

    private fun drawText(gc: GraphicsContext, text: DiagramElement.Text) {
        gc.fill = Color.web(text.style.fontColor, text.style.opacity)
        gc.font = Font.font(text.style.fontFamily, FontWeight.NORMAL, text.fontSize)
        gc.fillText(text.text, text.position.x, text.position.y)
    }

    private fun drawMermaidBlock(gc: GraphicsContext, block: DiagramElement.MermaidBlock) {
        val x = block.position.x
        val y = block.position.y
        val w = block.width
        val h = block.height

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
        val x = pdf.position.x
        val y = pdf.position.y
        val w = pdf.width
        val h = pdf.height

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
        val x = group.position.x
        val y = group.position.y
        gc.strokeRect(x, y, 100.0, 50.0)
        gc.fill = Color.rgb(0, 0, 0, 0.3)
        gc.font = Font.font("sans-serif", 10.0)
        gc.fillText("[Group: ${group.id}]", x + 4, y + 14)

        group.elements.forEach { element ->
            drawElement(gc, element)
        }
    }

    private fun handleClick(x: Double, y: Double) {
        selectedElementId = null
        for (element in document.elements.reversed()) {
            if (hitTest(element, x, y)) {
                selectedElementId = element.id
                break
            }
        }
        render()
    }

    private fun hitTest(element: DiagramElement, x: Double, y: Double): Boolean {
        return when (element) {
            is DiagramElement.Node -> {
                x >= element.position.x && x <= element.position.x + element.size.width &&
                    y >= element.position.y && y <= element.position.y + element.size.height
            }
            is DiagramElement.MermaidBlock -> {
                x >= element.position.x && x <= element.position.x + element.width &&
                    y >= element.position.y && y <= element.position.y + element.height
            }
            is DiagramElement.Text -> {
                val approxWidth = element.text.length * element.fontSize * 0.6
                x >= element.position.x && x <= element.position.x + approxWidth &&
                    y >= element.position.y - element.fontSize && y <= element.position.y
            }
            is DiagramElement.PdfElement -> {
                x >= element.position.x && x <= element.position.x + element.width &&
                    y >= element.position.y && y <= element.position.y + element.height
            }
            is DiagramElement.Edge -> false
            is DiagramElement.Group -> {
                element.elements.any { hitTest(it, x, y) }
            }
        }
    }

    private fun moveSelectedElement(dx: Double, dy: Double) {
        val element = document.elements.find { it.id == selectedElementId } ?: return
        val idx = document.elements.indexOf(element)
        val moved = when (element) {
            is DiagramElement.Node -> element.copy(position = Position(element.position.x + dx, element.position.y + dy))
            is DiagramElement.Edge -> element.copy(
                position = Position(element.position.x + dx, element.position.y + dy),
                waypoints = element.waypoints.map { wp ->
                    DiagramElement.EdgeWaypoint(Position(wp.position.x + dx, wp.position.y + dy))
                }
            )
            is DiagramElement.Text -> element.copy(position = Position(element.position.x + dx, element.position.y + dy))
            is DiagramElement.MermaidBlock -> element.copy(position = Position(element.position.x + dx, element.position.y + dy))
            is DiagramElement.PdfElement -> element.copy(position = Position(element.position.x + dx, element.position.y + dy))
            is DiagramElement.Group -> element.copy(position = Position(element.position.x + dx, element.position.y + dy))
        }
        document.elements[idx] = moved
        render()
    }
}
