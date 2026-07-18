package com.freegram.converter

import com.freegram.format.mermaid.MermaidWriter
import com.freegram.model.*

object FdmlToMermaidConverter {

    fun convert(doc: FdmlDocument): ConversionResult {
        val incompatibilities = mutableListOf<String>()
        val mermaidLines = mutableListOf<String>()

        val diagramType = detectMermaidType(doc)
        mermaidLines.add(diagramType)
        mermaidLines.add("")

        var hasPdfElement = false
        var hasExtendedBlock = false
        var hasPositionOnlyFeatures = false

        for (element in doc.elements) {
            when (element) {
                is DiagramElement.Node -> convertNode(element, mermaidLines)
                is DiagramElement.Edge -> convertEdge(element, mermaidLines)
                is DiagramElement.MermaidBlock -> {
                    if (element.isNativeMermaid) {
                        mermaidLines.add(element.mermaidCode)
                    } else {
                        hasExtendedBlock = true
                        incompatibilities.add("Extended FDML block '${element.id}' has no Mermaid equivalent")
                        convertExtendedBlockComment(element, mermaidLines)
                    }
                }
                is DiagramElement.Text -> {
                    hasPositionOnlyFeatures = true
                    incompatibilities.add("Free-floating text '${element.id}' position info is lost in Mermaid")
                    convertTextComment(element, mermaidLines)
                }
                is DiagramElement.PdfElement -> {
                    hasPdfElement = true
                    incompatibilities.add("PDF element '${element.id}' cannot be represented in Mermaid")
                    convertPdfComment(element, mermaidLines)
                }
                is DiagramElement.Group -> convertGroup(element, mermaidLines, incompatibilities)
            }
        }

        val level = when {
            hasPdfElement || hasExtendedBlock -> MermaidCompatibilityLevel.PARTIAL
            hasPositionOnlyFeatures -> MermaidCompatibilityLevel.PARTIAL
            else -> MermaidCompatibilityLevel.FULL
        }

        val result = mermaidLines.joinToString("\n")
        return ConversionResult(
            output = result,
            compatibility = MermaidCompatibility(level, incompatibilities.distinct())
        )
    }

    private fun detectMermaidType(doc: FdmlDocument): String {
        return "graph TD"
    }

    private fun convertNode(node: DiagramElement.Node, lines: MutableList<String>) {
        val id = MermaidWriter.sanitizeId(node.id)
        val label = if (node.content.isNotEmpty()) node.content else node.id
        val escaped = label.replace("\"", "#quot;")
            .replace(":", "#colon;")
        val shapeOpen = shapeToMermaid(node.style.shapeType)
        val shapeClose = shapeClose(node.style.shapeType)
        lines.add("    $id$shapeOpen$escaped$shapeClose")
    }

    private fun shapeToMermaid(shapeType: String): String {
        return when (shapeType) {
            "rectangle" -> "["
            "round-rect" -> "(["
            "circle" -> "(("
            "diamond" -> "{"
            "stadium" -> "(["
            "parallelogram" -> "[/"
            "parallelogram-alt" -> "[\\"
            "hexagon" -> "{{"
            else -> "["
        }
    }

    private fun shapeClose(shapeType: String): String {
        return when (shapeType) {
            "rectangle" -> "]"
            "round-rect" -> "])"
            "circle" -> "))"
            "diamond" -> "}"
            "stadium" -> "])"
            "parallelogram" -> "/]"
            "parallelogram-alt" -> "\\]"
            "hexagon" -> "}}"
            else -> "]"
        }
    }

    private fun convertEdge(edge: DiagramElement.Edge, lines: MutableList<String>) {
        val src = MermaidWriter.sanitizeId(edge.sourceId)
        val tgt = MermaidWriter.sanitizeId(edge.targetId)
        val arrow = when (edge.edgeType) {
            "arrow" -> "-->"
            "dotted" -> "-.->"
            "thick" -> "==>"
            "bidirectional" -> "<-->"
            "dashed" -> "-.->"
            else -> "-->"
        }
        val label = if (edge.label.isNotEmpty()) "|${edge.label}|" else ""
        lines.add("    $src $arrow$label $tgt")
    }

    private fun convertExtendedBlockComment(block: DiagramElement.MermaidBlock, lines: MutableList<String>) {
        lines.add("    %% FDML Extended Block: ${block.id}")
        lines.add("    %% This content is not representable in standard Mermaid")
    }

    private fun convertTextComment(text: DiagramElement.Text, lines: MutableList<String>) {
        lines.add("    %% Text note: ${text.text}")
    }

    private fun convertPdfComment(pdf: DiagramElement.PdfElement, lines: MutableList<String>) {
        lines.add("    %% PDF Element: ${pdf.id} (page ${pdf.pageNumber})")
    }

    private fun convertGroup(group: DiagramElement.Group, lines: MutableList<String>, incompatibilities: MutableList<String>) {
        lines.add("    subgraph ${MermaidWriter.sanitizeId(group.id)} [${group.id}]")
        group.elements.forEach { element ->
            when (element) {
                is DiagramElement.Node -> convertNode(element, lines)
                is DiagramElement.Edge -> convertEdge(element, lines)
                is DiagramElement.Text -> convertTextComment(element, lines)
                is DiagramElement.MermaidBlock -> {
                    if (element.isNativeMermaid) {
                        lines.add(element.mermaidCode)
                    } else {
                        incompatibilities.add("Extended FDML block in group '${group.id}'")
                        convertExtendedBlockComment(element, lines)
                    }
                }
                is DiagramElement.PdfElement -> {
                    incompatibilities.add("PDF element in group '${group.id}'")
                    convertPdfComment(element, lines)
                }
                is DiagramElement.Group -> convertGroup(element, lines, incompatibilities)
            }
        }
        lines.add("    end")
    }
}
