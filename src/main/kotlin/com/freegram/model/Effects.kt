package com.freegram.model

data class DropShadow(
    val enabled: Boolean = false,
    val offsetX: Double = 3.0,
    val offsetY: Double = 3.0,
    val radius: Double = 5.0,
    val color: String = "#000000",
    val opacity: Double = 0.3
)

data class GlowEffect(
    val enabled: Boolean = false,
    val radius: Double = 8.0,
    val color: String = "#00FF00",
    val opacity: Double = 0.5
)

data class GradientStop(
    val offset: Double = 0.0,
    val color: String = "#FFFFFF"
)

data class Gradient(
    val enabled: Boolean = false,
    val type: String = "linear",
    val angle: Double = 0.0,
    val stops: List<GradientStop> = listOf(
        GradientStop(0.0, "#FFFFFF"),
        GradientStop(1.0, "#CCCCCC")
    )
)

enum class EdgeDashPattern(val pattern: List<Float>) {
    SOLID(listOf()),
    DASHED(listOf(8f, 4f)),
    DOTTED(listOf(2f, 4f)),
    DASHDOT(listOf(8f, 4f, 2f, 4f)),
    LONGDASH(listOf(16f, 6f))
}

enum class EdgeArrowType(val label: String) {
    NONE("none"),
    SIMPLE("simple"),
    TRIANGLE("triangle"),
    DIAMOND("diamond"),
    CIRCLE("circle"),
    OPEN("open")
}

data class EdgeDecoration(
    val sourceArrow: EdgeArrowType = EdgeArrowType.NONE,
    val targetArrow: EdgeArrowType = EdgeArrowType.TRIANGLE,
    val dashPattern: EdgeDashPattern = EdgeDashPattern.SOLID,
    val curved: Boolean = false,
    val curvature: Double = 0.3
)

data class EnhancedStyle(
    val fillColor: String = "#FFFFFF",
    val strokeColor: String = "#000000",
    val strokeWidth: Double = 1.0,
    val fontFamily: String = "sans-serif",
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
