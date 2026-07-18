package com.freegram.ui

import com.freegram.converter.*
import com.freegram.format.fdml.FdmlWriter
import com.freegram.model.FdmlDocument
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.File

class ExportDialog(private val owner: Stage) {

    private val conversionManager = ConversionManager()

    fun showConversionDialog(doc: FdmlDocument) {
        val dialog = Stage()
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(owner)
        dialog.title = "FreeGram - Convert / Export"

        val root = VBox(12.0)
        root.padding = Insets(16.0)

        val sourceLabel = Label("Source Format:")
        val sourceCombo = ComboBox<Format>()
        sourceCombo.items.addAll(Format.FDML, Format.MERMAID, Format.PDF)
        sourceCombo.value = Format.FDML

        val targetLabel = Label("Target Format:")
        val targetCombo = ComboBox<Format>()
        targetCombo.items.addAll(Format.FDML, Format.MERMAID, Format.PDF)
        targetCombo.value = Format.MERMAID

        val pathLabel = Label("Conversion Path: FDML → Mermaid")
        sourceCombo.setOnAction {
            pathLabel.text = "${sourceCombo.value} → ${targetCombo.value}"
        }
        targetCombo.setOnAction {
            pathLabel.text = "${sourceCombo.value} → ${targetCombo.value}"
        }

        val convertButton = Button("Convert & Export")
        val resultArea = TextArea()
        resultArea.prefRowCount = 15
        resultArea.isEditable = false

        val saveButton = Button("Save to File...")
        saveButton.isDisable = true

        var lastResult: ConversionResult? = null

        convertButton.setOnAction {
            try {
                val sourceFormat = sourceCombo.value
                val targetFormat = targetCombo.value

                val result = when (sourceFormat) {
                    Format.FDML -> conversionManager.convert(
                        FdmlWriter.write(doc), Format.FDML, targetFormat
                    )
                    else -> {
                        resultArea.text = "Please export FDML first, then use that output for $sourceFormat conversions."
                        return@setOnAction
                    }
                }

                lastResult = result
                saveButton.isDisable = false

                val sb = StringBuilder()
                sb.appendLine("=== Conversion: ${sourceFormat} → ${targetFormat} ===")
                sb.appendLine("Compatibility: ${result.compatibility.level}")
                result.compatibility.reasons.forEach { sb.appendLine("  ⚠ $it") }
                sb.appendLine()

                when (targetFormat) {
                    Format.FDML -> {
                        val fdml = result.fdmlDocument?.let { FdmlWriter.write(it) } ?: result.output
                        sb.appendLine(fdml)
                        if (fdml.length > 2000) sb.appendLine("... (truncated, ${fdml.length} chars)")
                    }
                    Format.MERMAID -> {
                        sb.appendLine(result.output)
                    }
                    Format.PDF -> {
                        val size = result.pdfData?.size ?: 0
                        sb.appendLine("PDF generated: $size bytes")
                    }
                }
                resultArea.text = sb.toString()
            } catch (e: Exception) {
                resultArea.text = "Error: ${e.message}"
            }
        }

        saveButton.setOnAction {
            val targetFormat = targetCombo.value
            val result = lastResult ?: return@setOnAction

            val fileChooser = FileChooser()
            fileChooser.title = "Save as ${targetFormat.name}"

            when (targetFormat) {
                Format.FDML -> {
                    fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("FDML files", "*.fdml"))
                    val file = fileChooser.showSaveDialog(dialog) ?: return@setOnAction
                    val content = result.fdmlDocument?.let { FdmlWriter.write(it) } ?: result.output
                    file.writeText(content)
                }
                Format.MERMAID -> {
                    fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Mermaid files", "*.mmd"))
                    val file = fileChooser.showSaveDialog(dialog) ?: return@setOnAction
                    file.writeText(result.output)
                }
                Format.PDF -> {
                    fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("PDF files", "*.pdf"))
                    val file = fileChooser.showSaveDialog(dialog) ?: return@setOnAction
                    file.writeBytes(result.pdfData ?: return@setOnAction)
                }
            }
        }

        val closeButton = Button("Close")
        closeButton.setOnAction { dialog.close() }

        val buttonRow = HBox(8.0, convertButton, saveButton, closeButton)

        root.children.addAll(
            Label("FreeGram Format Converter"),
            Separator(),
            sourceLabel, sourceCombo,
            targetLabel, targetCombo,
            pathLabel,
            convertButton,
            Separator(),
            Label("Result:"),
            resultArea,
            buttonRow
        )

        dialog.scene = Scene(root, 700.0, 600.0)
        dialog.showAndWait()
    }

    fun showImportDialog(onImport: (String, ByteArray?, Format) -> Unit) {
        val dialog = Stage()
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(owner)
        dialog.title = "FreeGram - Import"

        val root = VBox(12.0)
        root.padding = Insets(16.0)

        val formatLabel = Label("Import Format:")
        val formatCombo = ComboBox<Format>()
        formatCombo.items.addAll(Format.FDML, Format.MERMAID, Format.PDF)
        formatCombo.value = Format.FDML

        val fileButton = Button("Select File...")
        val filePathLabel = Label("No file selected")

        var selectedFile: File? = null

        fileButton.setOnAction {
            val fileChooser = FileChooser()
            fileChooser.title = "Import File"
            val format = formatCombo.value
            when (format) {
                Format.FDML -> fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("FDML files", "*.fdml", "*.xml"))
                Format.MERMAID -> fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Mermaid files", "*.mmd", "*.mermaid"))
                Format.PDF -> fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("PDF files", "*.pdf"))
            }
            val file = fileChooser.showOpenDialog(dialog) ?: return@setOnAction
            selectedFile = file
            filePathLabel.text = file.absolutePath
        }

        val importButton = Button("Import")
        importButton.setOnAction {
            val file = selectedFile ?: return@setOnAction
            val format = formatCombo.value
            try {
                when (format) {
                    Format.FDML, Format.MERMAID -> {
                        val text = file.readText()
                        onImport(text, null, format)
                    }
                    Format.PDF -> {
                        val bytes = file.readBytes()
                        onImport("", bytes, format)
                    }
                }
                dialog.close()
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR, "Import failed: ${e.message}").showAndWait()
            }
        }

        val cancelButton = Button("Cancel")
        cancelButton.setOnAction { dialog.close() }

        val buttonRow = HBox(8.0, importButton, cancelButton)
        root.children.addAll(
            Label("Import Diagram"),
            Separator(),
            formatLabel, formatCombo,
            fileButton, filePathLabel,
            buttonRow
        )

        dialog.scene = Scene(root, 500.0, 300.0)
        dialog.showAndWait()
    }
}
