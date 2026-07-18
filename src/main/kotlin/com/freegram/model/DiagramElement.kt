package com.freegram.model

data class Style(
    val fillColor: String = "#FFFFFF",
    val strokeColor: String = "#000000",
    val strokeWidth: Double = 1.0,
    val fontFamily: String = "Noto Sans CJK KR",
    val fontSize: Double = 14.0,
    val fontColor: String = "#000000",
    val opacity: Double = 1.0,
    val shapeType: String = "rectangle",
    val dropShadow: DropShadow = DropShadow(),
    val glow: GlowEffect = GlowEffect(),
    val gradient: Gradient = Gradient(),
    val edgeDecoration: EdgeDecoration = EdgeDecoration(),
    val cornerRadius: Double = 5.0,
    val rotation: Double = 0.0,
    val strokeDash: String = "solid",
    val linkUrl: String = "",
    val tooltip: String = ""
)

object ShapeTypes {
    const val RECTANGLE = "rectangle"
    const val ROUND_RECT = "round-rect"
    const val CIRCLE = "circle"
    const val OVAL = "oval"
    const val DIAMOND = "diamond"
    const val STADIUM = "stadium"
    const val PARALLELOGRAM = "parallelogram"
    const val PARALLELOGRAM_ALT = "parallelogram-alt"
    const val TRAPEZOID = "trapezoid"
    const val TRAPEZOID_ALT = "trapezoid-alt"
    const val HEXAGON = "hexagon"
    const val PENTAGON = "pentagon"
    const val OCTAGON = "octagon"
    const val STAR = "star"
    const val STAR_4 = "star-4"
    const val STAR_6 = "star-6"
    const val ARROW_UP = "arrow-up"
    const val ARROW_DOWN = "arrow-down"
    const val ARROW_LEFT = "arrow-left"
    const val ARROW_RIGHT = "arrow-right"
    const val TRIANGLE = "triangle"
    const val TRIANGLE_DOWN = "triangle-down"
    const val CROSS = "cross"
    const val HEART = "heart"
    const val CLOUD = "cloud"
    const val CYLINDER = "cylinder"
    const val DOCUMENT = "document"
    const val NOTE = "note"
    const val PROCESS = "process"
    const val DELAY = "delay"
    const val MANUAL_INPUT = "manual-input"
    const val CARD = "card"
    const val OR = "or"

    val ALL = listOf(
        RECTANGLE, ROUND_RECT, CIRCLE, OVAL, DIAMOND, STADIUM,
        PARALLELOGRAM, PARALLELOGRAM_ALT, TRAPEZOID, TRAPEZOID_ALT,
        HEXAGON, PENTAGON, OCTAGON,
        STAR, STAR_4, STAR_6,
        ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT,
        TRIANGLE, TRIANGLE_DOWN,
        CROSS, HEART, CLOUD,
        CYLINDER, DOCUMENT, NOTE,
        PROCESS, DELAY, MANUAL_INPUT, CARD, OR
    )
}

sealed class DiagramElement {
    abstract val id: String
    abstract val position: Position
    abstract val style: Style
    abstract val layerId: String

    data class Node(
        override val id: String,
        override val position: Position,
        val size: Size = Size(100.0, 50.0),
        val content: String = "",
        override val style: Style = Style(),
        override val layerId: String = "default"
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
        override val style: Style = Style(),
        override val layerId: String = "default"
    ) : DiagramElement()

    data class MermaidBlock(
        override val id: String,
        override val position: Position,
        val mermaidCode: String,
        val width: Double = 600.0,
        val height: Double = 400.0,
        val isNativeMermaid: Boolean = true,
        override val style: Style = Style(),
        override val layerId: String = "default"
    ) : DiagramElement()

    data class PdfElement(
        override val id: String,
        override val position: Position,
        val pdfData: ByteArray,
        val pageNumber: Int = 1,
        val width: Double = 595.0,
        val height: Double = 842.0,
        override val style: Style = Style(),
        override val layerId: String = "default"
    ) : DiagramElement()

    data class Text(
        override val id: String,
        override val position: Position,
        val text: String,
        val fontSize: Double = 14.0,
        override val style: Style = Style(),
        override val layerId: String = "default"
    ) : DiagramElement()

    data class Group(
        override val id: String,
        override val position: Position,
        val elements: MutableList<DiagramElement> = mutableListOf(),
        override val style: Style = Style(),
        override val layerId: String = "default"
    ) : DiagramElement()
}
