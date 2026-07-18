package com.freegram.format.pdf

import com.freegram.model.*
import org.apache.pdfbox.pdmodel.*
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.*
import java.io.ByteArrayOutputStream

object PdfWriter {

    @Throws(Exception::class)
    fun write(doc: FdmlDocument): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val document = PDDocument()
        val page = PDPage(PDRectangle(doc.diagramWidth.toFloat(), doc.diagramHeight.toFloat()))
        document.addPage(page)

        val cs = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)

        cs.setNonStrokingColor(java.awt.Color.decode(doc.backgroundColor))
        cs.addRect(0f, 0f, doc.diagramWidth.toFloat(), doc.diagramHeight.toFloat())
        cs.fill()

        val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)

        for (element in doc.elements) {
            when (element) {
                is DiagramElement.Node -> writeNode(cs, element, font, doc)
                is DiagramElement.Edge -> writeEdge(cs, element, font)
                is DiagramElement.Text -> writeText(cs, element, font)
                is DiagramElement.MermaidBlock -> writeMermaidPlaceholder(cs, element, font)
                is DiagramElement.Group -> writeGroup(cs, element, font, doc)
                is DiagramElement.PdfElement -> writePdfElement(cs, element, font)
            }
        }

        cs.close()
        document.save(outputStream)
        document.close()
        return outputStream.toByteArray()
    }

    private fun writeNode(cs: PDPageContentStream, node: DiagramElement.Node, font: PDFont, doc: FdmlDocument) {
        val x = node.position.x.toFloat()
        val y = doc.diagramHeight.toFloat() - node.position.y.toFloat() - node.size.height.toFloat()
        val w = node.size.width.toFloat()
        val h = node.size.height.toFloat()

        cs.setNonStrokingColor(java.awt.Color.decode(node.style.fillColor))
        cs.setStrokingColor(java.awt.Color.decode(node.style.strokeColor))
        cs.setLineWidth(node.style.strokeWidth.toFloat())

        when (node.style.shapeType) {
            "circle" -> {
                val r = minOf(w, h) / 2f
                val cx = x + w / 2f
                val cy = y + h / 2f
                drawEllipse(cs, cx, cy, r, r)
            }
            "diamond" -> {
                cs.moveTo(x + w / 2, y)
                cs.lineTo(x + w, y + h / 2)
                cs.lineTo(x + w / 2, y + h)
                cs.lineTo(x, y + h / 2)
                cs.closePath()
            }
            "round-rect" -> {
                val arc = minOf(w, h) * 0.2f
                drawRoundRect(cs, x, y, w, h, arc)
            }
            else -> cs.addRect(x, y, w, h)
        }
        cs.fillAndStroke()

        if (node.content.isNotEmpty()) {
            cs.beginText()
            cs.setFont(font, node.style.fontSize.toFloat())
            cs.setNonStrokingColor(java.awt.Color.decode(node.style.fontColor))
            cs.newLineAtOffset(x + 4, y + h / 2 - node.style.fontSize.toFloat() / 2)
            cs.showText(node.content)
            cs.endText()
        }
    }

    private fun drawEllipse(cs: PDPageContentStream, cx: Float, cy: Float, rx: Float, ry: Float) {
        val kappa = 0.5522847498f
        cs.moveTo(cx + rx, cy)
        cs.curveTo(cx + rx, cy + kappa * ry, cx + kappa * rx, cy + ry, cx, cy + ry)
        cs.curveTo(cx - kappa * rx, cy + ry, cx - rx, cy + kappa * ry, cx - rx, cy)
        cs.curveTo(cx - rx, cy - kappa * ry, cx - kappa * rx, cy - ry, cx, cy - ry)
        cs.curveTo(cx + kappa * rx, cy - ry, cx + rx, cy - kappa * ry, cx + rx, cy)
    }

    private fun drawRoundRect(cs: PDPageContentStream, x: Float, y: Float, w: Float, h: Float, arc: Float) {
        val r = minOf(arc, minOf(w, h) / 2f)
        cs.moveTo(x + r, y)
        cs.lineTo(x + w - r, y)
        cs.curveTo(x + w, y, x + w, y + r, x + w, y + r)
        cs.lineTo(x + w, y + h - r)
        cs.curveTo(x + w, y + h, x + w - r, y + h, x + w - r, y + h)
        cs.lineTo(x + r, y + h)
        cs.curveTo(x, y + h, x, y + h - r, x, y + h - r)
        cs.lineTo(x, y + r)
        cs.curveTo(x, y, x + r, y, x + r, y)
    }

    private fun writeEdge(cs: PDPageContentStream, edge: DiagramElement.Edge, font: PDFont) {
        cs.setStrokingColor(java.awt.Color.decode(edge.style.strokeColor))
        cs.setLineWidth(edge.style.strokeWidth.toFloat())

        if (edge.waypoints.isNotEmpty()) {
            val first = edge.waypoints.first()
            cs.moveTo(first.position.x.toFloat(), first.position.y.toFloat())
            for (i in 1 until edge.waypoints.size) {
                cs.lineTo(edge.waypoints[i].position.x.toFloat(), edge.waypoints[i].position.y.toFloat())
            }
            cs.stroke()
        }

        if (edge.label.isNotEmpty()) {
            cs.beginText()
            cs.setFont(font, 10f)
            cs.setNonStrokingColor(java.awt.Color.decode(edge.style.fontColor))
            cs.newLineAtOffset(edge.position.x.toFloat() + 10, edge.position.y.toFloat() + 10)
            cs.showText(edge.label)
            cs.endText()
        }
    }

    private fun writeText(cs: PDPageContentStream, text: DiagramElement.Text, font: PDFont) {
        cs.beginText()
        cs.setFont(font, text.fontSize.toFloat())
        cs.setNonStrokingColor(java.awt.Color.decode(text.style.fontColor))
        cs.newLineAtOffset(text.position.x.toFloat(), text.position.y.toFloat())
        cs.showText(text.text)
        cs.endText()
    }

    private fun writeMermaidPlaceholder(cs: PDPageContentStream, block: DiagramElement.MermaidBlock, font: PDFont) {
        val x = block.position.x.toFloat()
        val y = block.position.y.toFloat()
        val w = block.width.toFloat()
        val h = block.height.toFloat()

        cs.setNonStrokingColor(java.awt.Color(200, 200, 255))
        cs.addRect(x, y, w, h)
        cs.fill()

        cs.setStrokingColor(java.awt.Color(100, 100, 200))
        cs.setLineWidth(1f)
        cs.addRect(x, y, w, h)
        cs.stroke()

        cs.beginText()
        cs.setFont(font, 10f)
        cs.setNonStrokingColor(java.awt.Color(50, 50, 100))
        cs.newLineAtOffset(x + 4, y + h - 14)
        cs.showText(if (block.isNativeMermaid) "[Mermaid Diagram]" else "[Extended FDML Block]")
        cs.endText()
    }

    private fun writeGroup(cs: PDPageContentStream, group: DiagramElement.Group, font: PDFont, doc: FdmlDocument) {
        group.elements.forEach { element ->
            when (element) {
                is DiagramElement.Node -> writeNode(cs, element, font, doc)
                is DiagramElement.Edge -> writeEdge(cs, element, font)
                is DiagramElement.Text -> writeText(cs, element, font)
                is DiagramElement.MermaidBlock -> writeMermaidPlaceholder(cs, element, font)
                is DiagramElement.Group -> writeGroup(cs, element, font, doc)
                else -> {}
            }
        }
    }

    private fun writePdfElement(cs: PDPageContentStream, pdfEl: DiagramElement.PdfElement, font: PDFont) {
        val x = pdfEl.position.x.toFloat()
        val y = pdfEl.position.y.toFloat()
        val w = pdfEl.width.toFloat()
        val h = pdfEl.height.toFloat()

        cs.setNonStrokingColor(java.awt.Color(240, 240, 240))
        cs.addRect(x, y, w, h)
        cs.fill()
        cs.setStrokingColor(java.awt.Color(150, 150, 150))
        cs.addRect(x, y, w, h)
        cs.stroke()
        cs.beginText()
        cs.setFont(font, 10f)
        cs.newLineAtOffset(x + 4, y + h / 2)
        cs.showText("[Embedded PDF - ${pdfEl.pdfData.size} bytes]")
        cs.endText()
    }
}
