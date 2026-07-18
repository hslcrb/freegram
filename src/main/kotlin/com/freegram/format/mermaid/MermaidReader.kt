package com.freegram.format.mermaid

import com.freegram.model.*
import java.util.*

object MermaidReader {

    fun read(mermaidCode: String): FdmlDocument {
        val doc = FdmlDocument(
            title = "Imported Mermaid Diagram",
            mermaidCompatibility = MermaidCompatibility(MermaidCompatibilityLevel.FULL)
        )

        val lines = mermaidCode.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("%%") }
        val typeLine = lines.firstOrNull { it.startsWith("graph") || it.startsWith("flowchart") || it.startsWith("sequenceDiagram") || it.startsWith("classDiagram") || it.startsWith("stateDiagram") || it.startsWith("gantt") || it.startsWith("pie") || it.startsWith("erDiagram") }
        var startIndex = 0
        if (typeLine != null) {
            startIndex = lines.indexOf(typeLine) + 1
        }

        val nodeMap = mutableMapOf<String, String>()
        var nodeCounter = 0

        var idx = startIndex
        while (idx < lines.size) {
            val line = lines[idx]
            if (line.startsWith("subgraph ")) {
                val endIdx = findEndSubgraph(lines, idx)
                if (endIdx > idx) {
                    idx = endIdx
                }
                idx++
                continue
            }
            if (line.startsWith("end")) { idx++; continue }

            val edgePattern = Regex("""(\w[\w\d_-]*)\s*(--?[->]|==?[=>]|-\.->|<=?>?--?|<--?)\s*(.*)""")
            val match = edgePattern.find(line)

            if (match != null) {
                val src = match.groupValues[1]
                val conn = match.groupValues[2]
                val rest = match.groupValues[3]
                val target = rest.split("|").lastOrNull()?.trim()?.let { it.split(Regex("\\s+")).firstOrNull() } ?: ""

                val srcNode = findOrCreateNode(doc, src, nodeMap, nodeCounter)
                if (srcNode != null) nodeCounter++
                val tgtNode = findOrCreateNode(doc, target, nodeMap, nodeCounter)
                if (tgtNode != null) nodeCounter++

                val edgeType = when {
                    conn.contains("==") -> "thick"
                    conn.contains("-.-") -> "dotted"
                    conn.contains("<->") || conn.contains("<-->") -> "bidirectional"
                    else -> "arrow"
                }

                val labelMatch = Regex("\\|([^|]+)\\|").find(line)
                val label = labelMatch?.groupValues?.getOrNull(1) ?: ""

                val edge = DiagramElement.Edge(
                    id = "e_${src}_${target}_${UUID.randomUUID().toString().take(8)}",
                    position = Position.ZERO,
                    sourceId = nodeMap[src] ?: src,
                    targetId = nodeMap[target] ?: target,
                    label = label,
                    edgeType = edgeType
                )
                if (edge.sourceId != edge.targetId) {
                    doc.elements.add(edge)
                }
            } else {
                val nodePattern = Regex("""(\w[\w\d_-]*)\s*(\[.*?]|\(.*?\)|\{.*?}|>.*?])""")
                val nodeMatch = nodePattern.find(line)
                if (nodeMatch != null) {
                    val id = nodeMatch.groupValues[1]
                    val shape = nodeMatch.groupValues[2]
                    val content = extractContent(shape)
                    if (!nodeMap.containsKey(id)) {
                        val nodeId = "n_${id}_${nodeCounter++}"
                        nodeMap[id] = nodeId
                        doc.elements.add(
                            DiagramElement.Node(
                                id = nodeId,
                                position = Position(100.0 * (nodeCounter % 10), 100.0 * (nodeCounter / 10)),
                                content = content,
                                style = Style(shapeType = detectShapeType(shape))
                            )
                        )
                    }
                } else if (line.contains("-->") || line.contains("---") || line.contains("==")) {
                } else {
                    val simpleId = Regex("""^\s*(\w[\w\d_-]*)\s*$""").find(line)
                    if (simpleId != null) {
                        findOrCreateNode(doc, simpleId.groupValues[1], nodeMap, nodeCounter)?.let { nodeCounter++ }
                    }
                }
            }
            idx++
        }

        return doc
    }

    private fun findEndSubgraph(lines: List<String>, start: Int): Int {
        var depth = 1
        for (i in start + 1 until lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("subgraph ")) depth++
            if (line == "end") {
                depth--
                if (depth == 0) return i
            }
        }
        return start
    }

    private fun findOrCreateNode(doc: FdmlDocument, id: String, nodeMap: MutableMap<String, String>, counter: Int): String? {
        if (id.isBlank()) return null
        if (nodeMap.containsKey(id)) return nodeMap[id]
        val nodeId = "n_${id}_${counter}"
        nodeMap[id] = nodeId
        doc.elements.add(
            DiagramElement.Node(
                id = nodeId,
                position = Position(100.0 * (counter % 10), 100.0 * (counter / 10)),
                content = id
            )
        )
        return nodeId
    }

    private fun extractContent(shape: String): String {
        return shape.removeSurrounding("[", "]")
            .removeSurrounding("(", ")")
            .removeSurrounding("{", "}")
            .removeSurrounding("(", "])")
            .trim()
    }

    private fun detectShapeType(shape: String): String {
        return when {
            shape.startsWith("[") -> "rectangle"
            shape.startsWith("([") -> "round-rect"
            shape.startsWith("((") -> "circle"
            shape.startsWith("{") -> "diamond"
            shape.startsWith("{{") -> "hexagon"
            shape.startsWith("[/") || shape.startsWith("[\\") -> "parallelogram"
            shape.startsWith(">") -> "stadium"
            else -> "rectangle"
        }
    }
}
