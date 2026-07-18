package com.freegram.ui

import com.freegram.model.*
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox

class PropertyPane : VBox() {

    private val titleLabel = Label("속성")
    private val nameField = TextField()
    private val xField = TextField()
    private val yField = TextField()
    private val widthField = TextField()
    private val heightField = TextField()
    private val contentField = TextArea()
    private val fillColorField = TextField()
    private val strokeColorField = TextField()
    private val fontFamilyField = TextField()
    private val fontSizeField = TextField()
    private val shapeTypeField = ComboBox<String>()
    private val opacitySlider = Slider(0.0, 1.0, 1.0)
    private val rotationField = TextField()
    private val strokeWidthField = TextField()
    private val strokeDashField = ComboBox<String>()
    private val shadowCheck = CheckBox("그림자")
    private val glowCheck = CheckBox("글로우")
    private val snapCheck = CheckBox("스냅")
    private val edgeTypeField = ComboBox<String>()
    private val arrowSourceField = ComboBox<String>()
    private val arrowTargetField = ComboBox<String>()
    private val dashPatternField = ComboBox<String>()

    private var currentElement: DiagramElement? = null
    private var currentDoc: FdmlDocument? = null

    init {
        spacing = 6.0
        padding = Insets(10.0)

        shapeTypeField.items.addAll(ShapeTypes.ALL)
        strokeDashField.items.addAll("solid", "dashed", "dotted", "dashdot")
        edgeTypeField.items.addAll("arrow", "dotted", "thick", "bidirectional", "dashed", "link")
        arrowSourceField.items.addAll("none", "simple", "triangle", "diamond", "circle", "open")
        arrowTargetField.items.addAll("triangle", "simple", "diamond", "circle", "open", "none")
        dashPatternField.items.addAll("solid", "dashed", "dotted", "dashdot", "longdash")

        opacitySlider.isShowTickLabels = true
        opacitySlider.isShowTickMarks = true

        val scrollContent = VBox(6.0)
        val tabs = TabPane()
        val basicTab = Tab("기본")
        val styleTab = Tab("스타일")
        val effectTab = Tab("효과")
        val edgeTab = Tab("엣지")

        basicTab.content = buildBasicPane()
        styleTab.content = buildStylePane()
        effectTab.content = buildEffectPane()
        edgeTab.content = buildEdgePane()
        tabs.tabs.addAll(basicTab, styleTab, effectTab, edgeTab)

        val updateButton = Button("적용")
        updateButton.setOnAction { applyChanges() }

        children.addAll(titleLabel, Separator(), tabs, updateButton)
    }

    private fun buildBasicPane(): VBox {
        val grid = GridPane()
        grid.hgap = 6.0; grid.vgap = 4.0
        grid.add(Label("ID:"), 0, 0); grid.add(nameField, 1, 0)
        grid.add(Label("X:"), 0, 1); grid.add(xField, 1, 1)
        grid.add(Label("Y:"), 0, 2); grid.add(yField, 1, 2)
        grid.add(Label("너비:"), 0, 3); grid.add(widthField, 1, 3)
        grid.add(Label("높이:"), 0, 4); grid.add(heightField, 1, 4)
        grid.add(Label("내용:"), 0, 5); grid.add(contentField, 1, 5)
        grid.add(Label("회전:"), 0, 6); grid.add(rotationField, 1, 6)
        grid.add(snapCheck, 1, 7)
        return VBox(grid)
    }

    private fun buildStylePane(): VBox {
        val grid = GridPane()
        grid.hgap = 6.0; grid.vgap = 4.0
        grid.add(Label("채우기:"), 0, 0); grid.add(fillColorField, 1, 0)
        grid.add(Label("선:"), 0, 1); grid.add(strokeColorField, 1, 1)
        grid.add(Label("선두께:"), 0, 2); grid.add(strokeWidthField, 1, 2)
        grid.add(Label("선패턴:"), 0, 3); grid.add(strokeDashField, 1, 3)
        grid.add(Label("폰트:"), 0, 4); grid.add(fontFamilyField, 1, 4)
        grid.add(Label("폰트크기:"), 0, 5); grid.add(fontSizeField, 1, 5)
        grid.add(Label("투명도:"), 0, 6); grid.add(opacitySlider, 1, 6)
        grid.add(Label("모양:"), 0, 7); grid.add(shapeTypeField, 1, 7)
        return VBox(grid)
    }

    private fun buildEffectPane(): VBox {
        return VBox(6.0,
            Label("효과"),
            shadowCheck,
            glowCheck
        )
    }

    private fun buildEdgePane(): VBox {
        val grid = GridPane()
        grid.hgap = 6.0; grid.vgap = 4.0
        grid.add(Label("엣지타입:"), 0, 0); grid.add(edgeTypeField, 1, 0)
        grid.add(Label("시작화살표:"), 0, 1); grid.add(arrowSourceField, 1, 1)
        grid.add(Label("끝화살표:"), 0, 2); grid.add(arrowTargetField, 1, 2)
        grid.add(Label("대시패턴:"), 0, 3); grid.add(dashPatternField, 1, 3)
        return VBox(grid)
    }

    fun showElement(element: DiagramElement?, doc: FdmlDocument?) {
        currentElement = element
        currentDoc = doc

        if (element == null) {
            nameField.text = ""; xField.text = ""; yField.text = ""
            widthField.text = ""; heightField.text = ""
            contentField.text = ""; fillColorField.text = ""
            strokeColorField.text = ""; fontFamilyField.text = ""
            fontSizeField.text = ""; rotationField.text = ""
            strokeWidthField.text = ""; shapeTypeField.value = null
            return
        }

        val s = element.style
        nameField.text = element.id
        xField.text = "%.0f".format(element.position.x)
        yField.text = "%.0f".format(element.position.y)
        fillColorField.text = s.fillColor
        strokeColorField.text = s.strokeColor
        fontFamilyField.text = s.fontFamily
        fontSizeField.text = "%.0f".format(s.fontSize)
        opacitySlider.value = s.opacity
        rotationField.text = "%.0f".format(s.rotation)
        strokeWidthField.text = "%.1f".format(s.strokeWidth)
        strokeDashField.value = s.strokeDash
        shapeTypeField.value = s.shapeType
        shadowCheck.isSelected = s.dropShadow.enabled
        glowCheck.isSelected = s.glow.enabled

        when (element) {
            is DiagramElement.Node -> {
                widthField.text = "%.0f".format(element.size.width)
                heightField.text = "%.0f".format(element.size.height)
                contentField.text = element.content
            }
            is DiagramElement.Edge -> {
                widthField.text = ""; heightField.text = ""
                contentField.text = element.label
                edgeTypeField.value = element.edgeType
                arrowSourceField.value = s.edgeDecoration.sourceArrow.label
                arrowTargetField.value = s.edgeDecoration.targetArrow.label
                dashPatternField.value = s.edgeDecoration.dashPattern.name.lowercase()
            }
            is DiagramElement.Text -> {
                widthField.text = ""; heightField.text = ""
                contentField.text = element.text
            }
            is DiagramElement.MermaidBlock -> {
                widthField.text = "%.0f".format(element.width)
                heightField.text = "%.0f".format(element.height)
                contentField.text = element.mermaidCode
            }
            is DiagramElement.PdfElement -> {
                widthField.text = "%.0f".format(element.width)
                heightField.text = "%.0f".format(element.height)
                contentField.text = "[PDF ${element.pdfData.size} bytes]"
            }
            is DiagramElement.Group -> {
                widthField.text = ""; heightField.text = ""
                contentField.text = "Group: ${element.elements.size} elements"
            }
        }
    }

    private fun applyChanges() {
        val element = currentElement ?: return
        val doc = currentDoc ?: return
        val idx = doc.elements.indexOf(element)
        if (idx < 0) return

        val x = xField.text.toDoubleOrNull() ?: element.position.x
        val y = yField.text.toDoubleOrNull() ?: element.position.y

        val shadow = DropShadow(enabled = shadowCheck.isSelected)
        val glow = GlowEffect(enabled = glowCheck.isSelected)
        val edgeDeco = EdgeDecoration(
            sourceArrow = parseArrowType(arrowSourceField.value),
            targetArrow = parseArrowType(arrowTargetField.value),
            dashPattern = parseDashPattern(dashPatternField.value)
        )

        val style = Style(
            fillColor = fillColorField.text.ifBlank { element.style.fillColor },
            strokeColor = strokeColorField.text.ifBlank { element.style.strokeColor },
            strokeWidth = strokeWidthField.text.toDoubleOrNull() ?: element.style.strokeWidth,
            fontFamily = fontFamilyField.text.ifBlank { element.style.fontFamily },
            fontSize = fontSizeField.text.toDoubleOrNull() ?: element.style.fontSize,
            fontColor = element.style.fontColor,
            opacity = opacitySlider.value,
            shapeType = shapeTypeField.value ?: element.style.shapeType,
            dropShadow = shadow,
            glow = glow,
            edgeDecoration = edgeDeco,
            rotation = rotationField.text.toDoubleOrNull() ?: element.style.rotation,
            strokeDash = strokeDashField.value ?: element.style.strokeDash
        )

        val updated = when (element) {
            is DiagramElement.Node -> {
                val w = widthField.text.toDoubleOrNull() ?: element.size.width
                val h = heightField.text.toDoubleOrNull() ?: element.size.height
                element.copy(position = Position(x, y), size = Size(w, h), content = contentField.text, style = style)
            }
            is DiagramElement.Edge -> element.copy(
                position = Position(x, y), label = contentField.text, style = style,
                edgeType = edgeTypeField.value ?: element.edgeType
            )
            is DiagramElement.Text -> element.copy(
                position = Position(x, y), text = contentField.text,
                fontSize = fontSizeField.text.toDoubleOrNull() ?: element.fontSize, style = style
            )
            is DiagramElement.MermaidBlock -> element.copy(
                position = Position(x, y), mermaidCode = contentField.text,
                width = widthField.text.toDoubleOrNull() ?: element.width,
                height = heightField.text.toDoubleOrNull() ?: element.height, style = style
            )
            is DiagramElement.PdfElement -> element.copy(
                position = Position(x, y), style = style
            )
            is DiagramElement.Group -> element.copy(position = Position(x, y), style = style)
        }

        doc.elements[idx] = updated
        currentDoc = doc
    }

    private fun parseArrowType(value: String?): EdgeArrowType {
        return when (value?.lowercase()) {
            "none" -> EdgeArrowType.NONE
            "simple" -> EdgeArrowType.SIMPLE
            "triangle" -> EdgeArrowType.TRIANGLE
            "diamond" -> EdgeArrowType.DIAMOND
            "circle" -> EdgeArrowType.CIRCLE
            "open" -> EdgeArrowType.OPEN
            else -> EdgeArrowType.TRIANGLE
        }
    }

    private fun parseDashPattern(value: String?): EdgeDashPattern {
        return when (value?.lowercase()) {
            "dashed" -> EdgeDashPattern.DASHED
            "dotted" -> EdgeDashPattern.DOTTED
            "dashdot" -> EdgeDashPattern.DASHDOT
            "longdash" -> EdgeDashPattern.LONGDASH
            else -> EdgeDashPattern.SOLID
        }
    }
}
