package com.freegram.model

data class Style(
    val fillColor: String = "#FFFFFF",
    val strokeColor: String = "#000000",
    val strokeWidth: Double = 1.0,
    val fontFamily: String = "sans-serif",
    val fontSize: Double = 14.0,
    val fontColor: String = "#000000",
    val opacity: Double = 1.0,
    val shapeType: String = "rectangle"
)

sealed class DiagramElement {
    abstract val id: String
    abstract val position: Position
    abstract val style: Style

    data class Node(
        override val id: String,
        override val position: Position,
        val size: Size = Size(100.0, 50.0),
        val content: String = "",
        override val style: Style = Style()
    ) : DiagramElement()

    data class EdgeWaypoint(
        val position: Position
    )

    data class Edge(
        override val id: String,
        override val position: Position,
        val sourceId: String,
        val targetId: String,
        val label: String = "",
        val waypoints: List<EdgeWaypoint> = emptyList(),
        val edgeType: String = "arrow",
        override val style: Style = Style()
    ) : DiagramElement()

    data class MermaidBlock(
        override val id: String,
        override val position: Position,
        val mermaidCode: String,
        val width: Double = 600.0,
        val height: Double = 400.0,
        val isNativeMermaid: Boolean = true,
        override val style: Style = Style()
    ) : DiagramElement()

    data class PdfElement(
        override val id: String,
        override val position: Position,
        val pdfData: ByteArray,
        val pageNumber: Int = 1,
        val width: Double = 595.0,
        val height: Double = 842.0,
        override val style: Style = Style()
    ) : DiagramElement()

    data class Text(
        override val id: String,
        override val position: Position,
        val text: String,
        val fontSize: Double = 14.0,
        override val style: Style = Style()
    ) : DiagramElement()

    data class Group(
        override val id: String,
        override val position: Position,
        val elements: MutableList<DiagramElement> = mutableListOf(),
        override val style: Style = Style()
    ) : DiagramElement()
}
