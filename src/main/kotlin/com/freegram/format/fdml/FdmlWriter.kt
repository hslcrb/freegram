package com.freegram.format.fdml

import com.freegram.model.*
import org.jdom2.*
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.io.StringWriter
import java.time.format.DateTimeFormatter
import java.util.*

object FdmlWriter {

    fun write(doc: FdmlDocument): String {
        val root = Element("fdml").apply {
            setAttribute("version", doc.version)
            setAttribute("xmlns", "https://freegram.dev/fdml/1.0")
        }

        root.addContent(buildMetadata(doc))
        root.addContent(buildResources(doc))
        root.addContent(buildDiagram(doc))

        val xmlDoc = Document(root)
        val outputter = XMLOutputter(Format.getPrettyFormat())
        val writer = StringWriter()
        outputter.output(xmlDoc, writer)
        return writer.toString()
    }

    private fun buildMetadata(doc: FdmlDocument): Element {
        val meta = Element("metadata")
        meta.addContent(Element("title").setText(doc.title))
        meta.addContent(Element("author").setText(doc.author))
        meta.addContent(Element("description").setText(doc.description))
        meta.addContent(Element("created").setText(doc.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
        meta.addContent(Element("modified").setText(doc.modifiedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
        meta.addContent(Element("backgroundColor").setText(doc.backgroundColor))

        val compat = Element("mermaidCompatibility").setAttribute("level", doc.mermaidCompatibility.level.name.lowercase())
        doc.mermaidCompatibility.reasons.forEach { reason ->
            compat.addContent(Element("incompatibleReason").setText(reason))
        }
        meta.addContent(compat)

        return meta
    }

    private fun buildResources(doc: FdmlDocument): Element {
        val resources = Element("resources")
        doc.resources.forEach { (id, resource) ->
            val el = when (resource) {
                is EmbeddedResource.Font -> Element("font").apply {
                    setAttribute("id", id)
                    setAttribute("fontFamily", resource.fontFamily)
                    setAttribute("fontWeight", resource.fontWeight)
                    setAttribute("fontStyle", resource.fontStyle)
                    setAttribute("mimeType", resource.mimeType)
                    setText(Base64.getEncoder().encodeToString(resource.data))
                }
                is EmbeddedResource.Image -> Element("image").apply {
                    setAttribute("id", id)
                    setAttribute("mimeType", resource.mimeType)
                    setText(Base64.getEncoder().encodeToString(resource.data))
                }
                is EmbeddedResource.Raw -> Element("embed").apply {
                    setAttribute("id", id)
                    setAttribute("mimeType", resource.mimeType)
                    setAttribute("description", resource.description)
                    setText(Base64.getEncoder().encodeToString(resource.data))
                }
            }
            resources.addContent(el)
        }
        return resources
    }

    private fun buildDiagram(doc: FdmlDocument): Element {
        val diagram = Element("diagram").apply {
            setAttribute("width", doc.diagramWidth.toString())
            setAttribute("height", doc.diagramHeight.toString())
            if (doc.gridEnabled) {
                setAttribute("gridSize", doc.gridSize.toString())
            }
        }
        doc.elements.forEach { element ->
            diagram.addContent(buildElement(element))
        }
        return diagram
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildElement(element: DiagramElement): Element {
        return when (element) {
            is DiagramElement.Node -> buildNode(element)
            is DiagramElement.Edge -> buildEdge(element)
            is DiagramElement.MermaidBlock -> buildMermaidBlock(element)
            is DiagramElement.PdfElement -> buildPdfElement(element)
            is DiagramElement.Text -> buildText(element)
            is DiagramElement.Group -> buildGroup(element)
        }
    }

    private fun buildStyle(el: Element, style: Style) {
        val s = Element("style")
        s.setAttribute("fillColor", style.fillColor)
        s.setAttribute("strokeColor", style.strokeColor)
        s.setAttribute("strokeWidth", style.strokeWidth.toString())
        s.setAttribute("fontFamily", style.fontFamily)
        s.setAttribute("fontSize", style.fontSize.toString())
        s.setAttribute("fontColor", style.fontColor)
        s.setAttribute("opacity", style.opacity.toString())
        s.setAttribute("shapeType", style.shapeType)
        el.addContent(s)
    }

    private fun buildNode(node: DiagramElement.Node): Element {
        val el = Element("node").apply {
            setAttribute("id", node.id)
            setAttribute("x", node.position.x.toString())
            setAttribute("y", node.position.y.toString())
            setAttribute("width", node.size.width.toString())
            setAttribute("height", node.size.height.toString())
        }
        buildStyle(el, node.style)
        el.addContent(Element("content").setText(node.content))
        return el
    }

    private fun buildEdge(edge: DiagramElement.Edge): Element {
        val el = Element("edge").apply {
            setAttribute("id", edge.id)
            setAttribute("source", edge.sourceId)
            setAttribute("target", edge.targetId)
            setAttribute("type", edge.edgeType)
            setAttribute("x", edge.position.x.toString())
            setAttribute("y", edge.position.y.toString())
        }
        buildStyle(el, edge.style)
        if (edge.label.isNotEmpty()) {
            el.addContent(Element("label").setText(edge.label))
        }
        edge.waypoints.forEach { wp ->
            el.addContent(Element("waypoint").apply {
                setAttribute("x", wp.position.x.toString())
                setAttribute("y", wp.position.y.toString())
            })
        }
        return el
    }

    private fun buildMermaidBlock(block: DiagramElement.MermaidBlock): Element {
        val el = Element("mermaid").apply {
            setAttribute("id", block.id)
            setAttribute("x", block.position.x.toString())
            setAttribute("y", block.position.y.toString())
            setAttribute("width", block.width.toString())
            setAttribute("height", block.height.toString())
            setAttribute("native", block.isNativeMermaid.toString())
        }
        el.addContent(Element("code").addContent(CDATA(block.mermaidCode)))
        return el
    }

    private fun buildPdfElement(pdf: DiagramElement.PdfElement): Element {
        val el = Element("pdf").apply {
            setAttribute("id", pdf.id)
            setAttribute("x", pdf.position.x.toString())
            setAttribute("y", pdf.position.y.toString())
            setAttribute("page", pdf.pageNumber.toString())
            setAttribute("width", pdf.width.toString())
            setAttribute("height", pdf.height.toString())
            setText(Base64.getEncoder().encodeToString(pdf.pdfData))
        }
        return el
    }

    private fun buildText(text: DiagramElement.Text): Element {
        val el = Element("text").apply {
            setAttribute("id", text.id)
            setAttribute("x", text.position.x.toString())
            setAttribute("y", text.position.y.toString())
            setAttribute("fontSize", text.fontSize.toString())
        }
        buildStyle(el, text.style)
        el.addContent(Element("content").setText(text.text))
        return el
    }

    private fun buildGroup(group: DiagramElement.Group): Element {
        val el = Element("group").apply {
            setAttribute("id", group.id)
            setAttribute("x", group.position.x.toString())
            setAttribute("y", group.position.y.toString())
        }
        buildStyle(el, group.style)
        group.elements.forEach { child ->
            el.addContent(buildElement(child))
        }
        return el
    }
}
