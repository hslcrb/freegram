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
        val doc = FdmlDocument(version = root.getAttributeValue("version") ?: "1.0")

        root.children.forEach { child ->
            when (child.name) {
                "metadata" -> parseMetadata(child, doc)
                "resources" -> parseResources(child, doc)
                "layers" -> parseLayers(child, doc)
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
                "created" -> try { doc.createdAt = LocalDateTime.parse(child.textNormalize, DateTimeFormatter.ISO_LOCAL_DATE_TIME) } catch (_: Exception) {}
                "modified" -> try { doc.modifiedAt = LocalDateTime.parse(child.textNormalize, DateTimeFormatter.ISO_LOCAL_DATE_TIME) } catch (_: Exception) {}
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
            doc.resources[id] = when (child.name) {
                "font" -> EmbeddedResource.Font(id = id, data = data, fontFamily = child.getAttributeValue("fontFamily") ?: "unknown",
                    fontWeight = child.getAttributeValue("fontWeight") ?: "normal", fontStyle = child.getAttributeValue("fontStyle") ?: "normal", mimeType = mime)
                "image" -> EmbeddedResource.Image(id = id, data = data, mimeType = mime)
                "embed" -> EmbeddedResource.Raw(id = id, data = data, mimeType = mime, description = child.getAttributeValue("description") ?: "")
                else -> EmbeddedResource.Raw(id = id, data = data, mimeType = mime)
            }
        }
    }

    private fun parseLayers(el: Element, doc: FdmlDocument) {
        doc.layers.clear()
        el.children.forEach { child ->
            doc.layers.add(Layer(
                id = child.getAttributeValue("id") ?: UUID.randomUUID().toString().take(8),
                name = child.getAttributeValue("name") ?: "레이어",
                visible = child.getAttributeValue("visible")?.toBooleanStrictOrNull() ?: true,
                locked = child.getAttributeValue("locked")?.toBooleanStrictOrNull() ?: false,
                order = child.getAttributeValue("order")?.toIntOrNull() ?: 0
            ))
        }
        if (doc.layers.isEmpty()) doc.layers.add(Layer("default", "기본 레이어"))
    }

    private fun parseDiagram(el: Element, doc: FdmlDocument) {
        doc.diagramWidth = el.getAttributeValue("width")?.toDoubleOrNull() ?: 1200.0
        doc.diagramHeight = el.getAttributeValue("height")?.toDoubleOrNull() ?: 900.0
        el.children.forEach { child -> parseElement(child)?.let { doc.elements.add(it) } }
    }

    private fun parseElement(el: Element): DiagramElement? = when (el.name) {
        "node" -> parseNode(el)
        "edge" -> parseEdge(el)
        "mermaid" -> parseMermaidBlock(el)
        "pdf" -> parsePdfElement(el)
        "text" -> parseText(el)
        "group" -> parseGroup(el)
        else -> null
    }

    private fun parseStyle(el: Element): Style {
        val s = el.getChild("style") ?: return Style()
        val shadowEl = s.getChild("shadow")
        val glowEl = s.getChild("glow")
        val gradientEl = s.getChild("gradient")
        val decoEl = s.getChild("edgeDecoration")

        val shadow = if (shadowEl != null) DropShadow(
            enabled = true,
            offsetX = shadowEl.getAttributeValue("offsetX")?.toDoubleOrNull() ?: 3.0,
            offsetY = shadowEl.getAttributeValue("offsetY")?.toDoubleOrNull() ?: 3.0,
            radius = shadowEl.getAttributeValue("radius")?.toDoubleOrNull() ?: 5.0,
            color = shadowEl.getAttributeValue("color") ?: "#000000",
            opacity = shadowEl.getAttributeValue("opacity")?.toDoubleOrNull() ?: 0.3
        ) else DropShadow()

        val glow = if (glowEl != null) GlowEffect(
            enabled = true,
            radius = glowEl.getAttributeValue("radius")?.toDoubleOrNull() ?: 8.0,
            color = glowEl.getAttributeValue("color") ?: "#00FF00",
            opacity = glowEl.getAttributeValue("opacity")?.toDoubleOrNull() ?: 0.5
        ) else GlowEffect()

        val gradient = if (gradientEl != null) {
            val stops = gradientEl.children.filter { it.name == "stop" }.map { stop ->
                GradientStop(
                    offset = stop.getAttributeValue("offset")?.toDoubleOrNull() ?: 0.0,
                    color = stop.getAttributeValue("color") ?: "#FFFFFF"
                )
            }
            Gradient(
                enabled = true,
                type = gradientEl.getAttributeValue("type") ?: "linear",
                angle = gradientEl.getAttributeValue("angle")?.toDoubleOrNull() ?: 0.0,
                stops = stops.ifEmpty { listOf(GradientStop(0.0, "#FFFFFF"), GradientStop(1.0, "#CCCCCC")) }
            )
        } else Gradient()

        val edgeDeco = if (decoEl != null) EdgeDecoration(
            sourceArrow = parseArrowType(decoEl.getAttributeValue("sourceArrow")),
            targetArrow = parseArrowType(decoEl.getAttributeValue("targetArrow")),
            dashPattern = parseDashPattern(decoEl.getAttributeValue("dashPattern")),
            curved = decoEl.getAttributeValue("curved")?.toBooleanStrictOrNull() ?: false
        ) else EdgeDecoration()

        return Style(
            fillColor = s.getAttributeValue("fillColor") ?: "#FFFFFF",
            strokeColor = s.getAttributeValue("strokeColor") ?: "#000000",
            strokeWidth = s.getAttributeValue("strokeWidth")?.toDoubleOrNull() ?: 1.0,
            strokeDash = s.getAttributeValue("strokeDash") ?: "solid",
            fontFamily = s.getAttributeValue("fontFamily") ?: "Noto Sans CJK KR",
            fontSize = s.getAttributeValue("fontSize")?.toDoubleOrNull() ?: 14.0,
            fontColor = s.getAttributeValue("fontColor") ?: "#000000",
            opacity = s.getAttributeValue("opacity")?.toDoubleOrNull() ?: 1.0,
            shapeType = s.getAttributeValue("shapeType") ?: "rectangle",
            cornerRadius = s.getAttributeValue("cornerRadius")?.toDoubleOrNull() ?: 5.0,
            rotation = s.getAttributeValue("rotation")?.toDoubleOrNull() ?: 0.0,
            dropShadow = shadow, glow = glow, gradient = gradient,
            edgeDecoration = edgeDeco,
            linkUrl = s.getAttributeValue("linkUrl") ?: "",
            tooltip = s.getAttributeValue("tooltip") ?: ""
        )
    }

    private fun parseArrowType(v: String?): EdgeArrowType = when (v?.lowercase()) {
        "none" -> EdgeArrowType.NONE; "simple" -> EdgeArrowType.SIMPLE; "triangle" -> EdgeArrowType.TRIANGLE
        "diamond" -> EdgeArrowType.DIAMOND; "circle" -> EdgeArrowType.CIRCLE; "open" -> EdgeArrowType.OPEN
        else -> EdgeArrowType.TRIANGLE
    }

    private fun parseDashPattern(v: String?): EdgeDashPattern = when (v?.lowercase()) {
        "dashed" -> EdgeDashPattern.DASHED; "dotted" -> EdgeDashPattern.DOTTED
        "dashdot" -> EdgeDashPattern.DASHDOT; "longdash" -> EdgeDashPattern.LONGDASH
        else -> EdgeDashPattern.SOLID
    }

    private fun parseLayerId(el: Element): String = el.getAttributeValue("layer") ?: "default"

    private fun parseNode(el: Element): DiagramElement.Node = DiagramElement.Node(
        id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
        position = Position(el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0, el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0),
        size = Size(el.getAttributeValue("width")?.toDoubleOrNull() ?: 100.0, el.getAttributeValue("height")?.toDoubleOrNull() ?: 50.0),
        content = el.getChildTextNormalize("content") ?: "",
        style = parseStyle(el), layerId = parseLayerId(el)
    )

    private fun parseEdge(el: Element): DiagramElement.Edge = DiagramElement.Edge(
        id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
        position = Position(el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0, el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0),
        sourceId = el.getAttributeValue("source") ?: "", targetId = el.getAttributeValue("target") ?: "",
        label = el.getChildTextNormalize("label") ?: "",
        waypoints = el.children.filter { it.name == "waypoint" }.map { wp -> DiagramElement.EdgeWaypoint(Position(wp.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0, wp.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0)) },
        edgeType = el.getAttributeValue("type") ?: "arrow",
        style = parseStyle(el), layerId = parseLayerId(el)
    )

    private fun parseMermaidBlock(el: Element): DiagramElement.MermaidBlock = DiagramElement.MermaidBlock(
        id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
        position = Position(el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0, el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0),
        mermaidCode = el.getChildTextNormalize("code") ?: "",
        width = el.getAttributeValue("width")?.toDoubleOrNull() ?: 600.0,
        height = el.getAttributeValue("height")?.toDoubleOrNull() ?: 400.0,
        isNativeMermaid = el.getAttributeValue("native")?.toBooleanStrictOrNull() ?: true,
        style = parseStyle(el), layerId = parseLayerId(el)
    )

    private fun parsePdfElement(el: Element): DiagramElement.PdfElement = DiagramElement.PdfElement(
        id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
        position = Position(el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0, el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0),
        pdfData = Base64.getDecoder().decode(el.textNormalize),
        pageNumber = el.getAttributeValue("page")?.toIntOrNull() ?: 1,
        width = el.getAttributeValue("width")?.toDoubleOrNull() ?: 595.0,
        height = el.getAttributeValue("height")?.toDoubleOrNull() ?: 842.0,
        style = parseStyle(el), layerId = parseLayerId(el)
    )

    private fun parseText(el: Element): DiagramElement.Text = DiagramElement.Text(
        id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
        position = Position(el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0, el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0),
        text = el.getChildTextNormalize("content") ?: "",
        fontSize = el.getAttributeValue("fontSize")?.toDoubleOrNull() ?: 14.0,
        style = parseStyle(el), layerId = parseLayerId(el)
    )

    private fun parseGroup(el: Element): DiagramElement.Group {
        val group = DiagramElement.Group(
            id = el.getAttributeValue("id") ?: UUID.randomUUID().toString(),
            position = Position(el.getAttributeValue("x")?.toDoubleOrNull() ?: 0.0, el.getAttributeValue("y")?.toDoubleOrNull() ?: 0.0),
            style = parseStyle(el), layerId = parseLayerId(el)
        )
        el.children.forEach { child -> if (child.name != "style") parseElement(child)?.let { group.elements.add(it) } }
        return group
    }
}
