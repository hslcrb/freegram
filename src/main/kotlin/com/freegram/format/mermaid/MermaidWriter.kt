package com.freegram.format.mermaid

import com.freegram.model.*

object MermaidWriter {

    fun write(doc: FdmlDocument): String {
        val sb = StringBuilder()

        detectDiagramType(doc)?.let { sb.appendLine(it) }
        sb.appendLine()

        doc.elements.forEach { element ->
            when (element) {
                is DiagramElement.Node -> writeNode(element, sb)
                is DiagramElement.Edge -> writeEdge(element, sb)
                is DiagramElement.MermaidBlock -> {
                    if (element.isNativeMermaid) {
                        sb.appendLine(element.mermaidCode)
                    } else {
                        writeExtendedBlock(element, sb)
                    }
                }
                is DiagramElement.Text -> writeText(element, sb)
                is DiagramElement.Group -> writeGroup(element, sb)
                is DiagramElement.PdfElement -> writePdfPlaceholder(element, sb)
            }
        }

        return sb.toString()
    }

    private fun detectDiagramType(doc: FdmlDocument): String? {
        val hasFlowNodes = doc.elements.any { it is DiagramElement.Node }
        val hasEdges = doc.elements.any { it is DiagramElement.Edge }
        val hasMermaid = doc.elements.any { it is DiagramElement.MermaidBlock && it.isNativeMermaid }

        if (hasMermaid) return null
        return if (hasFlowNodes && hasEdges) "graph TD" else null
    }

    private fun escapeMermaid(text: String): String {
        return text.replace("\"", "#quot;")
            .replace("\n", "<br/>")
            .replace("(", "&#40;")
            .replace(")", "&#41;")
    }

    private fun getShapeOpen(style: Style): String {
        return when (style.shapeType) {
            "rectangle" -> "["
            "round-rect" -> "(["
            "circle" -> "(("
            "diamond" -> "{"
            "stadium" -> "(["
            "parallelogram" -> "[/"
            "parallelogram-alt" -> "[\\"
            "trapezoid" -> "[/"
            "trapezoid-alt" -> "[\\"
            "hexagon" -> "{{"
            else -> "["
        }
    }

    private fun getShapeClose(style: Style): String {
        return when (style.shapeType) {
            "rectangle" -> "]"
            "round-rect" -> "])"
            "circle" -> "))"
            "diamond" -> "}"
            "stadium" -> "])"
            "parallelogram" -> "/]"
            "parallelogram-alt" -> "\\]"
            "trapezoid" -> "/]"
            "trapezoid-alt" -> "\\]"
            "hexagon" -> "}}"
            else -> "]"
        }
    }

    private fun writeNode(node: DiagramElement.Node, sb: StringBuilder) {
        val id = sanitizeId(node.id)
        val label = escapeMermaid(node.content.ifEmpty { node.id })
        val shapeOpen = getShapeOpen(node.style)
        val shapeClose = getShapeClose(node.style)
        sb.appendLine("    $id$shapeOpen$label$shapeClose")
    }

    private fun writeEdge(edge: DiagramElement.Edge, sb: StringBuilder) {
        val srcId = sanitizeId(edge.sourceId)
        val tgtId = sanitizeId(edge.targetId)
        val arrow = when (edge.edgeType) {
            "arrow" -> "-->"
            "dotted" -> "-.->"
            "thick" -> "==>"
            "bidirectional" -> "<-->"
            "dashed" -> "-.->"
            "link" -> "---"
            else -> "-->"
        }
        val label = if (edge.label.isNotEmpty()) "|${escapeMermaid(edge.label)}|" else ""
        sb.appendLine("    $srcId $arrow$label $tgtId")
    }

    private fun writeText(text: DiagramElement.Text, sb: StringBuilder) {
        sb.appendLine("    %% Text: ${escapeMermaid(text.text)}")
    }

    private fun writeGroup(group: DiagramElement.Group, sb: StringBuilder) {
        sb.appendLine("    subgraph ${sanitizeId(group.id)} [${escapeMermaid(group.id)}]")
        group.elements.forEach { element ->
            when (element) {
                is DiagramElement.Node -> writeNode(element, sb)
                is DiagramElement.Edge -> writeEdge(element, sb)
                is DiagramElement.Text -> writeText(element, sb)
                is DiagramElement.Group -> writeGroup(element, sb)
                else -> {}
            }
        }
        sb.appendLine("    end")
    }

    private fun writeExtendedBlock(block: DiagramElement.MermaidBlock, sb: StringBuilder) {
        sb.appendLine("    %% FDML Extended Block (not native mermaid)")
        sb.appendLine("    %% Position: x=${block.position.x}, y=${block.position.y}")
        sb.appendLine("    %% ${block.mermaidCode.lines().joinToString("\n    %% ") { it }}")
    }

    private fun writePdfPlaceholder(pdf: DiagramElement.PdfElement, sb: StringBuilder) {
        sb.appendLine("    %% PDF Element: ${pdf.id}")
        sb.appendLine("    %% Page: ${pdf.pageNumber}, embedded size: ${pdf.pdfData.size} bytes")
    }

    fun sanitizeId(id: String): String {
        return id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }
}
