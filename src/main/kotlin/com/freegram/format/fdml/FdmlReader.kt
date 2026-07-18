package com.freegram.format.fdml

import com.freegram.model.*
import org.jdom2.input.SAXBuilder
import org.jdom2.*
import java.io.StringReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object FdmlReader {

    fun read(xmlContent: String): FdmlDocument {
        val builder = SAXBuilder()
        val xmlDoc = builder.build(StringReader(xmlContent))
        val root = xmlDoc.rootElement

        val doc = FdmlDocument(
            version = root.getAttributeValue("version") ?: "1.0"
        )

        root.children.forEach { child ->
            when (child.name) {
                "metadata" -> parseMetadata(child, doc)
                "resources" -> parseResources(child, doc)
                "diagram" -> parseDiagram(child, doc)
            }
        }

        return doc
    }

    private fun parseMetadata(el: Element, doc: FdmlDocument) {
        el.children.forEach { child ->
            when (child.name) {
                "title" -> doc.title = child.textNormalize
                "author" -> doc.author = child.textNormalize
                "description" -> doc.description = child.textNormalize
                "created" -> {
                    try {
                        doc.createdAt = LocalDateTime.parse(child.textNormalize, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    } catch (_: Exception) {}
                }
                "modified" -> {
                    try {
                        doc.modifiedAt = LocalDateTime.parse(child.textNormalize, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    } catch (_: Exception) {}
                }
                "backgroundColor" -> doc.backgroundColor = child.textNormalize
                "mermaidCompatibility" -> parseCompatibility(child, doc)
            }
        }
    }

    private fun parseCompatibility(el: Element, doc: FdmlDocument) {
        val level = when (el.getAttributeValue("level")?.lowercase()) {
            "full" -> MermaidCompatibilityLevel.FULL
            "partial" -> MermaidCompatibilityLevel.PARTIAL
            "none" -> MermaidCompatibilityLevel.NONE
            else -> MermaidCompatibilityLevel.FULL
        }
        val reasons = el.children.filter { it.name == "incompatibleReason" }.map { it.textNormalize }
        doc.mermaidCompatibility = MermaidCompatibility(level, reasons)
    }

    private fun parseResources(el: Element, doc: FdmlDocument) {
        el.children.forEach { child ->
            val id = child.getAttributeValue("id") ?: return@forEach
            val mime = child.getAttributeValue("mimeType") ?: "application/octet-stream"
            val data = Base64.getDecoder().decode(child.textNormalize)

            val resource = when (child.name) {
                "font" -> EmbeddedResource.Font(
                    id = id,
                    data = data,
                    fontFamily = child.getAttributeValue("fontFamily") ?: "unknown",
                    fontWeight = child.getAttributeValue("fontWeight") ?: "normal",
                    fontStyle = child.getAttributeValue("fontStyle") ?: "normal",
                    mimeType = mime
                )
                "image" -> EmbeddedResource.Image(id = id, data = data, mimeType = mime)
                "embed" -> EmbeddedResource.Raw(
                    id = id, data = data, mimeType = mime,
                    description = child.getAttributeValue("description") ?: ""
                )
                else -> EmbeddedResource.Raw(id = id, data = data, mimeType = mime)
            }
            doc.resources[id] = resource
        }
    }

    private fun parseDiagram(el: Element, doc: FdmlDocument) {
        doc.diagramWidth = el.getAttributeValue("width")?.toDoubleOrNull() ?: 1200.0
        doc.diagramHeight = el.getAttributeValue("height")?.toDoubleOrNull() ?: 900.0
        doc.gridSize = el.getAttributeValue("gridSize")?.toDoubleOrNull() ?: 20.0

        el.children.forEach { child ->
            val element = parseElement(child) ?: return@forEach
            doc.elements.add(element)
        }
    }

    private fun parseElement(el: Element): DiagramElement? {
        return when (el.name) {
            "node" -> parseNode(el)
            "edge" -> parseEdge(el)
            "mermaid" -> parseMermaidBlock(el)
            "pdf" -> parsePdfElement(el)
            "text" -> parseText(el)
            "group" -> parseGroup(el)
            else -> null
        }
    }

    private fun parseStyle(el: Element): Style {
        val s = el.getChild("style") ?: return Style()
        return Style(
            fillColor = s.getAttributeValue("fillColor") ?: "#FFFFFF",
            strokeColor = s.getAttributeValue("strokeColor") ?: "#000000",
            strokeWidth = s.getAttributeValue("strokeWidth")?.toDoubleOrNull() ?: 1.0,
            fontFamily = s.getAttributeValue("fontFamily") ?: "sans-serif",
            fontSize = s.getAttributeValue("fontSize")?.toDoubleOrNull() ?: 14.0,
            fontColor = s.getAttributeValue("fontColor") ?: "#000000",
            opacity = s.getAttributeValue("opacity")?.toDoubleOrNull() ?: 1.0,
            shapeType = s.getAttributeValue("shapeType") ?: "rectangle"
        )
    }

    private fun parseNode(el: Element): DiagramElement.Node {
        return DiagramElement.Node(
            id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
            position = Position(
                el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0,
                el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0
            ),
            size = Size(
                el.getAttributeValue("width")?.toDoubleOrNull() ?: 100.0,
                el.getAttributeValue("height")?.toDoubleOrNull() ?: 50.0
            ),
            content = el.getChildTextNormalize("content") ?: "",
            style = parseStyle(el)
        )
    }

    private fun parseEdge(el: Element): DiagramElement.Edge {
        val waypoints = el.children.filter { it.name == "waypoint" }.map { wp ->
            DiagramElement.EdgeWaypoint(
                Position(
                    wp.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0,
                    wp.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0
                )
            )
        }
        return DiagramElement.Edge(
            id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
            position = Position(
                el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0,
                el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0
            ),
            sourceId = el.getAttributeValue("source") ?: "",
            targetId = el.getAttributeValue("target") ?: "",
            label = el.getChildTextNormalize("label") ?: "",
            waypoints = waypoints,
            edgeType = el.getAttributeValue("type") ?: "arrow",
            style = parseStyle(el)
        )
    }

    private fun parseMermaidBlock(el: Element): DiagramElement.MermaidBlock {
        return DiagramElement.MermaidBlock(
            id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
            position = Position(
                el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0,
                el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0
            ),
            mermaidCode = el.getChildTextNormalize("code") ?: "",
            width = el.getAttributeValue("width")?.toDoubleOrNull() ?: 600.0,
            height = el.getAttributeValue("height")?.toDoubleOrNull() ?: 400.0,
            isNativeMermaid = el.getAttributeValue("native")?.toBooleanStrictOrNull() ?: true,
            style = parseStyle(el)
        )
    }

    private fun parsePdfElement(el: Element): DiagramElement.PdfElement {
        val data = Base64.getDecoder().decode(el.textNormalize)
        return DiagramElement.PdfElement(
            id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
            position = Position(
                el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0,
                el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0
            ),
            pdfData = data,
            pageNumber = el.getAttributeValue("page")?.toIntOrNull() ?: 1,
            width = el.getAttributeValue("width")?.toDoubleOrNull() ?: 595.0,
            height = el.getAttributeValue("height")?.toDoubleOrNull() ?: 842.0,
            style = parseStyle(el)
        )
    }

    private fun parseText(el: Element): DiagramElement.Text {
        return DiagramElement.Text(
            id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
            position = Position(
                el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0,
                el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0
            ),
            text = el.getChildTextNormalize("content") ?: "",
            fontSize = el.getAttributeValue("fontSize")?.toDoubleOrNull() ?: 14.0,
            style = parseStyle(el)
        )
    }

    private fun parseGroup(el: Element): DiagramElement.Group {
        val group = DiagramElement.Group(
            id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
            position = Position(
                el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0,
                el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0
            ),
            style = parseStyle(el)
        )
        el.children.forEach { child ->
            if (child.name != "style") {
                parseElement(child)?.let { group.elements.add(it) }
            }
        }
        return group
    }
}
