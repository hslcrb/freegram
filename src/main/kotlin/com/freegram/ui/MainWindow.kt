package com.freegram.ui

import com.freegram.converter.*
import com.freegram.format.fdml.FdmlReader
import com.freegram.format.fdml.FdmlWriter
import com.freegram.model.*
import javafx.application.Platform
import javafx.geometry.Orientation
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.*
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.time.LocalDateTime
import java.util.*

class MainWindow(private val stage: Stage) {

    private val document: FdmlDocument = FdmlDocument()
    private var currentFile: File? = null

    private val canvas = DiagramCanvas()
    private val propertyPane = PropertyPane()
    private val previewPane = PreviewPane()
    private val exportDialog = ExportDialog(stage)

    private val statusBar = Label("Ready")

    fun show() {
        stage.title = "FreeGram - Diagram Editor"
        stage.minWidth = 1000.0
        stage.minHeight = 700.0

        val menuBar = buildMenuBar()

        val toolbar = buildToolbar()

        val scrollCanvas = ScrollPane(canvas).apply {
            isFitToWidth = false
            isFitToHeight = false
            isPannable = true
        }

        val leftPanel = VBox(8.0).apply {
            children.addAll(
                Label("Document Outline"),
                Separator(),
                TreeView<DiagramElement>().apply {
                    val rootItem = TreeItem<DiagramElement>(null)
                    rootItem.isExpanded = true
                    document.elements.forEach { el ->
                        rootItem.children.add(TreeItem(el))
                    }
                    root = rootItem
                }
            )
        }

        val centerPanel = BorderPane().apply {
            center = scrollCanvas
        }

        val rightPanel = VBox(8.0).apply {
            children.addAll(propertyPane, Separator(), previewPane)
            prefWidth = 350.0
        }

        val splitH = SplitPane().apply {
            items.addAll(leftPanel, centerPanel, rightPanel)
            setDividerPositions(0.15, 0.6)
        }

        val root = BorderPane().apply {
            top = VBox(menuBar, toolbar)
            center = splitH
            bottom = statusBar
        }

        canvas.setDocument(document)

        val scene = Scene(root)
        stage.scene = scene
        stage.show()
    }

    private fun buildMenuBar(): MenuBar {
        val fileMenu = Menu("_File").apply {
            items.addAll(
                MenuItem("_New").apply {
                    accelerator = KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN)
                    setOnAction { newDocument() }
                },
                MenuItem("_Open...").apply {
                    accelerator = KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN)
                    setOnAction { openDocument() }
                },
                MenuItem("_Save").apply {
                    accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN)
                    setOnAction { saveDocument() }
                },
                MenuItem("Save _As...").apply {
                    accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
                    setOnAction { saveDocumentAs() }
                },
                SeparatorMenuItem(),
                MenuItem("_Import...").apply {
                    setOnAction { showImportDialog() }
                },
                MenuItem("_Export / Convert...").apply {
                    accelerator = KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN)
                    setOnAction { showExportDialog() }
                },
                SeparatorMenuItem(),
                MenuItem("E_xit").apply {
                    accelerator = KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN)
                    setOnAction { Platform.exit() }
                }
            )
        }

        val editMenu = Menu("_Edit").apply {
            items.addAll(
                MenuItem("_Undo"),
                MenuItem("_Redo"),
                SeparatorMenuItem(),
                MenuItem("Add _Node").apply {
                    accelerator = KeyCodeCombination(KeyCode.N, KeyCombination.ALT_DOWN)
                    setOnAction { addNode() }
                },
                MenuItem("Add _Edge").apply {
                    accelerator = KeyCodeCombination(KeyCode.E, KeyCombination.ALT_DOWN)
                    setOnAction { addEdge() }
                },
                MenuItem("Add _Mermaid Block").apply {
                    accelerator = KeyCodeCombination(KeyCode.M, KeyCombination.ALT_DOWN)
                    setOnAction { addMermaidBlock() }
                },
                MenuItem("Add _Text").apply {
                    accelerator = KeyCodeCombination(KeyCode.T, KeyCombination.ALT_DOWN)
                    setOnAction { addText() }
                },
                SeparatorMenuItem(),
                MenuItem("_Delete Selected").apply {
                    accelerator = KeyCodeCombination(KeyCode.DELETE, KeyCombination.CONTROL_DOWN)
                    setOnAction { deleteSelected() }
                }
            )
        }

        val viewMenu = Menu("_View").apply {
            items.addAll(
                CheckMenuItem("Show _Grid").apply {
                    isSelected = document.gridEnabled
                    setOnAction { document.gridEnabled = isSelected; canvas.render() }
                },
                MenuItem("_Fit to Window"),
                MenuItem("_Zoom In"),
                MenuItem("Zoom _Out")
            )
        }

        val toolsMenu = Menu("_Tools").apply {
            items.addAll(
                MenuItem("_Convert to Mermaid...").apply { setOnAction { showExportDialog() } },
                MenuItem("_Import Mermaid...").apply { setOnAction { showImportDialog() } },
                SeparatorMenuItem(),
                MenuItem("_Batch Convert..."),
                MenuItem("Validate _FDML")
            )
        }

        val helpMenu = Menu("_Help").apply {
            items.addAll(
                MenuItem("_About FreeGram").apply {
                    setOnAction {
                        Alert(Alert.AlertType.INFORMATION,
                            "FreeGram v1.0\nDiagram Document Editor\nApache 2.0 License"
                        ).showAndWait()
                    }
                },
                MenuItem("FDML _Format Reference")
            )
        }

        return MenuBar(fileMenu, editMenu, viewMenu, toolsMenu, helpMenu)
    }

    private fun buildToolbar(): ToolBar {
        return ToolBar(
            Button("New").apply { setOnAction { newDocument() } },
            Button("Open").apply { setOnAction { openDocument() } },
            Button("Save").apply { setOnAction { saveDocument() } },
            Separator(),
            Button("Node").apply { setOnAction { addNode() } },
            Button("Edge").apply { setOnAction { addEdge() } },
            Button("Mermaid").apply { setOnAction { addMermaidBlock() } },
            Button("Text").apply { setOnAction { addText() } },
            Separator(),
            Button("Export").apply { setOnAction { showExportDialog() } },
            Button("Import").apply { setOnAction { showImportDialog() } }
        )
    }

    private fun newDocument() {
        document.elements.clear()
        document.resources.clear()
        document.title = "Untitled Diagram"
        document.createdAt = LocalDateTime.now()
        document.modifiedAt = LocalDateTime.now()
        currentFile = null
        canvas.setDocument(document)
        propertyPane.showElement(null, document)
        statusBar.text = "New document"
    }

    private fun openDocument() {
        val fileChooser = FileChooser().apply {
            title = "Open FDML File"
            extensionFilters.add(FileChooser.ExtensionFilter("FDML files", "*.fdml", "*.xml"))
        }
        val file = fileChooser.showOpenDialog(stage) ?: return
        try {
            val content = file.readText()
            val doc = FdmlReader.read(content)
            document.elements.clear()
            document.elements.addAll(doc.elements)
            document.resources.clear()
            document.resources.putAll(doc.resources)
            document.title = doc.title
            document.diagramWidth = doc.diagramWidth
            document.diagramHeight = doc.diagramHeight
            currentFile = file
            canvas.setDocument(document)
            previewPane.updatePreview(document)
            statusBar.text = "Opened: ${file.name}"
        } catch (e: Exception) {
            Alert(Alert.AlertType.ERROR, "Failed to open file: ${e.message}").showAndWait()
        }
    }

    private fun saveDocument(): Boolean {
        if (currentFile != null) return saveToFile(currentFile!!)
        return saveDocumentAs()
    }

    private fun saveDocumentAs(): Boolean {
        val fileChooser = FileChooser().apply {
            title = "Save FDML File"
            extensionFilters.add(FileChooser.ExtensionFilter("FDML files", "*.fdml"))
        }
        val file = fileChooser.showSaveDialog(stage) ?: return false
        currentFile = file
        return saveToFile(file)
    }

    private fun saveToFile(file: File): Boolean {
        return try {
            document.modifiedAt = LocalDateTime.now()
            val content = FdmlWriter.write(document)
            file.writeText(content)
            statusBar.text = "Saved: ${file.name}"
            true
        } catch (e: Exception) {
            Alert(Alert.AlertType.ERROR, "Failed to save file: ${e.message}").showAndWait()
            false
        }
    }

    private fun addNode() {
        val id = "node_${UUID.randomUUID().toString().take(8)}"
        val count = document.elements.filterIsInstance<DiagramElement.Node>().size
        val node = DiagramElement.Node(
            id = id,
            position = Position(100.0 + (count % 5) * 150.0, 100.0 + (count / 5) * 120.0),
            size = Size(140.0, 60.0),
            content = "Node ${count + 1}",
            style = Style(fillColor = "#4CAF50", strokeColor = "#2E7D32")
        )
        document.elements.add(node)
        canvas.render()
        statusBar.text = "Added node: $id"
    }

    private fun addEdge() {
        val nodes = document.elements.filterIsInstance<DiagramElement.Node>()
        if (nodes.size < 2) {
            Alert(Alert.AlertType.WARNING, "Need at least 2 nodes to create an edge").showAndWait()
            return
        }
        val source = nodes[nodes.size - 2]
        val target = nodes.last()
        val id = "edge_${UUID.randomUUID().toString().take(8)}"
        val edge = DiagramElement.Edge(
            id = id,
            position = Position(
                (source.position.x + source.size.width + target.position.x) / 2,
                (source.position.y + target.position.y) / 2
            ),
            sourceId = source.id,
            targetId = target.id,
            label = "edge",
            edgeType = "arrow",
            style = Style(strokeColor = "#333333", strokeWidth = 1.5)
        )
        document.elements.add(edge)
        canvas.render()
        statusBar.text = "Added edge: $id"
    }

    private fun addMermaidBlock() {
        val id = "mermaid_${UUID.randomUUID().toString().take(8)}"
        val count = document.elements.filterIsInstance<DiagramElement.MermaidBlock>().size
        val block = DiagramElement.MermaidBlock(
            id = id,
            position = Position(100.0 + (count % 3) * 250.0, 100.0 + (count / 3) * 200.0),
            mermaidCode = "graph TD\n    A[Start] --> B[End]",
            width = 400.0,
            height = 300.0,
            isNativeMermaid = true
        )
        document.elements.add(block)
        canvas.render()
        previewPane.updatePreview(document)
        statusBar.text = "Added Mermaid block: $id"
    }

    private fun addText() {
        val id = "text_${UUID.randomUUID().toString().take(8)}"
        val text = DiagramElement.Text(
            id = id,
            position = Position(200.0, 200.0),
            text = "Free-floating text",
            fontSize = 14.0,
            style = Style(fontColor = "#333333")
        )
        document.elements.add(text)
        canvas.render()
        statusBar.text = "Added text: $id"
    }

    private fun deleteSelected() {
        val selectedId = canvas.getDocument().elements.lastOrNull()?.id ?: return
        document.elements.removeAll { it.id == selectedId }
        canvas.render()
        statusBar.text = "Deleted element"
    }

    private fun showExportDialog() {
        exportDialog.showConversionDialog(document)
    }

    private fun showImportDialog() {
        exportDialog.showImportDialog importLoop@{ content, binary, format ->
            try {
                when (format) {
                    Format.FDML -> {
                        val doc = FdmlReader.read(content)
                        document.elements.clear()
                        document.elements.addAll(doc.elements)
                        document.title = doc.title
                        canvas.setDocument(document)
                        previewPane.updatePreview(document)
                        statusBar.text = "Imported FDML"
                    }
                    Format.MERMAID -> {
                        val result = MermaidToFdmlConverter.convert(content)
                        val doc = result.fdmlDocument ?: return@importLoop
                        document.elements.clear()
                        document.elements.addAll(doc.elements)
                        document.title = "Imported Mermaid Diagram"
                        document.mermaidCompatibility = result.compatibility
                        canvas.setDocument(document)
                        previewPane.updatePreview(document)
                        statusBar.text = "Imported Mermaid (compatibility: ${result.compatibility.level})"
                    }
                    Format.PDF -> {
                        if (binary != null) {
                            val result = PdfToFdmlConverter.convert(binary)
                            val doc = result.fdmlDocument ?: return@importLoop
                            document.elements.clear()
                            document.elements.addAll(doc.elements)
                            document.title = "Imported PDF"
                            document.mermaidCompatibility = result.compatibility
                            canvas.setDocument(document)
                            previewPane.updatePreview(document)
                            statusBar.text = "Imported PDF (${binary.size} bytes)"
                        }
                    }
                }
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR, "Import failed: ${e.message}").showAndWait()
            }
        }
    }
}
