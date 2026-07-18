package com.freegram.ui

import com.freegram.model.*
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox

class PropertyPane : VBox() {

    private val titleLabel = Label("Properties")
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

    private var currentElement: DiagramElement? = null
    private var currentDoc: FdmlDocument? = null

    init {
        spacing = 8.0
        padding = Insets(12.0)

        shapeTypeField.items.addAll(
            "rectangle", "round-rect", "circle", "diamond",
            "stadium", "parallelogram", "hexagon"
        )

        val grid = GridPane()
        grid.hgap = 8.0
        grid.vgap = 6.0

        grid.add(Label("ID/Name:"), 0, 0)
        grid.add(nameField, 1, 0)
        grid.add(Label("X:"), 0, 1)
        grid.add(xField, 1, 1)
        grid.add(Label("Y:"), 0, 2)
        grid.add(yField, 1, 2)
        grid.add(Label("Width:"), 0, 3)
        grid.add(widthField, 1, 3)
        grid.add(Label("Height:"), 0, 4)
        grid.add(heightField, 1, 4)
        grid.add(Label("Content:"), 0, 5)
        grid.add(contentField, 1, 5)
        grid.add(Label("Fill:"), 0, 6)
        grid.add(fillColorField, 1, 6)
        grid.add(Label("Stroke:"), 0, 7)
        grid.add(strokeColorField, 1, 7)
        grid.add(Label("Font:"), 0, 8)
        grid.add(fontFamilyField, 1, 8)
        grid.add(Label("Font Size:"), 0, 9)
        grid.add(fontSizeField, 1, 9)
        grid.add(Label("Shape:"), 0, 10)
        grid.add(shapeTypeField, 1, 10)

        val updateButton = Button("Apply")
        updateButton.setOnAction { applyChanges() }

        children.addAll(titleLabel, Separator(), grid, updateButton)
    }

    fun showElement(element: DiagramElement?, doc: FdmlDocument?) {
        currentElement = element
        currentDoc = doc

        if (element == null) {
            nameField.text = ""
            xField.text = ""
            yField.text = ""
            widthField.text = ""
            heightField.text = ""
            contentField.text = ""
            fillColorField.text = ""
            strokeColorField.text = ""
            fontFamilyField.text = ""
            fontSizeField.text = ""
            shapeTypeField.value = null
            return
        }

        nameField.text = element.id
        xField.text = element.position.x.toString()
        yField.text = element.position.y.toString()

        when (element) {
            is DiagramElement.Node -> {
                widthField.text = element.size.width.toString()
                heightField.text = element.size.height.toString()
                contentField.text = element.content
                fillColorField.text = element.style.fillColor
                strokeColorField.text = element.style.strokeColor
                fontFamilyField.text = element.style.fontFamily
                fontSizeField.text = element.style.fontSize.toString()
                shapeTypeField.value = element.style.shapeType
            }
            is DiagramElement.Edge -> {
                widthField.text = ""
                heightField.text = ""
                contentField.text = element.label
                fillColorField.text = element.style.fillColor
                strokeColorField.text = element.style.strokeColor
                fontFamilyField.text = element.style.fontFamily
                fontSizeField.text = element.style.fontSize.toString()
                shapeTypeField.value = element.style.shapeType
            }
            is DiagramElement.Text -> {
                widthField.text = ""
                heightField.text = ""
                contentField.text = element.text
                fillColorField.text = element.style.fillColor
                strokeColorField.text = element.style.strokeColor
                fontFamilyField.text = element.style.fontFamily
                fontSizeField.text = element.fontSize.toString()
                shapeTypeField.value = element.style.shapeType
            }
            is DiagramElement.MermaidBlock -> {
                widthField.text = element.width.toString()
                heightField.text = element.height.toString()
                contentField.text = element.mermaidCode
                fillColorField.text = element.style.fillColor
                strokeColorField.text = element.style.strokeColor
                fontFamilyField.text = element.style.fontFamily
                fontSizeField.text = element.style.fontSize.toString()
                shapeTypeField.value = element.style.shapeType
            }
            is DiagramElement.PdfElement -> {
                widthField.text = element.width.toString()
                heightField.text = element.height.toString()
                contentField.text = "[PDF ${element.pdfData.size} bytes]"
                fillColorField.text = element.style.fillColor
                strokeColorField.text = element.style.strokeColor
                fontFamilyField.text = element.style.fontFamily
                fontSizeField.text = element.style.fontSize.toString()
                shapeTypeField.value = element.style.shapeType
            }
            is DiagramElement.Group -> {
                widthField.text = ""
                heightField.text = ""
                contentField.text = "Group: ${element.elements.size} elements"
                fillColorField.text = element.style.fillColor
                strokeColorField.text = element.style.strokeColor
                fontFamilyField.text = element.style.fontFamily
                fontSizeField.text = element.style.fontSize.toString()
                shapeTypeField.value = element.style.shapeType
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
        val style = Style(
            fillColor = fillColorField.text.ifBlank { element.style.fillColor },
            strokeColor = strokeColorField.text.ifBlank { element.style.strokeColor },
            strokeWidth = element.style.strokeWidth,
            fontFamily = fontFamilyField.text.ifBlank { element.style.fontFamily },
            fontSize = fontSizeField.text.toDoubleOrNull() ?: element.style.fontSize,
            fontColor = element.style.fontColor,
            opacity = element.style.opacity,
            shapeType = shapeTypeField.value ?: element.style.shapeType
        )

        val updated = when (element) {
            is DiagramElement.Node -> element.copy(
                position = Position(x, y),
                content = contentField.text,
                size = Size(
                    widthField.text.toDoubleOrNull() ?: element.size.width,
                    heightField.text.toDoubleOrNull() ?: element.size.height
                ),
                style = style
            )
            is DiagramElement.Edge -> element.copy(
                position = Position(x, y),
                label = contentField.text,
                style = style
            )
            is DiagramElement.Text -> element.copy(
                position = Position(x, y),
                text = contentField.text,
                fontSize = fontSizeField.text.toDoubleOrNull() ?: element.fontSize,
                style = style
            )
            is DiagramElement.MermaidBlock -> element.copy(
                position = Position(x, y),
                mermaidCode = contentField.text,
                width = widthField.text.toDoubleOrNull() ?: element.width,
                height = heightField.text.toDoubleOrNull() ?: element.height,
                style = style
            )
            is DiagramElement.PdfElement -> element.copy(
                position = Position(x, y),
                width = widthField.text.toDoubleOrNull() ?: element.width,
                height = heightField.text.toDoubleOrNull() ?: element.height,
                style = style
            )
            is DiagramElement.Group -> element.copy(
                position = Position(x, y),
                style = style
            )
        }

        doc.elements[idx] = updated
        currentDoc = doc
    }
}
