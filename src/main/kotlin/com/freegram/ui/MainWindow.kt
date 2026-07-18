package com.freegram.ui

import com.freegram.converter.*
import com.freegram.format.fdml.FdmlReader
import com.freegram.format.fdml.FdmlWriter
import com.freegram.model.*
import javafx.application.Platform
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
    private val pdfEditor = PdfEditorPane(stage)
    private val undoManager = UndoManager()

    private val statusBar = Label("Ready")
    private val layerCombo = ComboBox<String>()
    private val zoomLabel = Label("100%")

    fun show() {
        stage.title = "FreeGram - Diagram Editor"
        stage.minWidth = 1100.0
        stage.minHeight = 750.0

        val menuBar = buildMenuBar()
        val toolbar = buildToolbar()
        val scrollCanvas = ScrollPane(canvas).apply { isFitToWidth = false; isFitToHeight = false; isPannable = true }

        canvas.undoAction = { undo() }
        canvas.redoAction = { redo() }
        canvas.onSelectionChanged = { el -> propertyPane.showElement(el, document) }
        canvas.onDocumentModified = { captureUndo() }

        val leftPanel = VBox(8.0).apply {
            children.addAll(
                Label("레이어"),
                Separator(),
                layerCombo,
                Button("레이어 추가").apply { setOnAction { addLayer() } },
                Separator(),
                Label("문서 개요"),
                TreeView<DiagramElement>().apply {
                    val rootItem = TreeItem<DiagramElement>(null)
                    rootItem.isExpanded = true
                    document.elements.forEach { rootItem.children.add(TreeItem(it)) }
                    root = rootItem
                }
            )
        }

        val rightPanel = VBox(6.0).apply {
            children.addAll(propertyPane, Separator(), previewPane)
            prefWidth = 380.0
        }

        val splitH = SplitPane().apply {
            items.addAll(leftPanel, centerPanel(scrollCanvas), rightPanel)
            setDividerPositions(0.12, 0.55)
        }

        val root = BorderPane().apply {
            top = VBox(menuBar, toolbar)
            center = splitH
            bottom = buildStatusBar()
        }

        canvas.setDocument(document)
        layerCombo.items.add("default")
        layerCombo.selectionModel.select("default")

        val scene = Scene(root)
        stage.scene = scene
        stage.show()
        canvas.requestCanvasFocus()
    }

    private fun centerPanel(scroll: ScrollPane): BorderPane {
        return BorderPane().apply { center = scroll }
    }

    private fun buildStatusBar(): HBox {
        return HBox(16.0, statusBar, Label("|"), zoomLabel, Label("|"),
            Button("-").apply { setOnAction { zoomOut() } },
            Button("+").apply { setOnAction { zoomIn() } },
            Button("Fit").apply { setOnAction { canvas.zoomToFit() } }
        ).apply { style = "-fx-padding: 2 8; -fx-background-color: #f0f0f0;" }
    }

    private fun captureUndo() {
        undoManager.capture(document)
        statusBar.text = "수정됨 (실행취소: ${undoManager.undoCount})"
    }

    private fun buildMenuBar(): MenuBar {
        val fileMenu = Menu("_파일").apply {
            items.addAll(
                MenuItem("_새 문서").apply { accelerator = KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN); setOnAction { newDocument() } },
                MenuItem("_열기...").apply { accelerator = KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN); setOnAction { openDocument() } },
                MenuItem("_저장").apply { accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN); setOnAction { saveDocument() } },
                MenuItem("다른 이름으로 저장...").apply { accelerator = KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN); setOnAction { saveDocumentAs() } },
                SeparatorMenuItem(),
                MenuItem("_가져오기...").apply { setOnAction { showImportDialog() } },
                MenuItem("내보내기/변환...").apply { accelerator = KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN); setOnAction { showExportDialog() } },
                SeparatorMenuItem(),
                MenuItem("종료").apply { accelerator = KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN); setOnAction { Platform.exit() } }
            )
        }

        val editMenu = Menu("_편집").apply {
            items.addAll(
                MenuItem("실행취소").apply { accelerator = KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN); setOnAction { undo() } },
                MenuItem("재실행").apply { accelerator = KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN); setOnAction { redo() } },
                SeparatorMenuItem(),
                MenuItem("복사").apply { accelerator = KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN); setOnAction { canvas.copySelected() } },
                MenuItem("붙여넣기").apply { accelerator = KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN); setOnAction { canvas.pasteClipboard() } },
                MenuItem("잘라내기").apply { accelerator = KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN); setOnAction { canvas.copySelected(); canvas.deleteSelected() } },
                MenuItem("삭제").apply { accelerator = KeyCodeCombination(KeyCode.DELETE, KeyCombination.CONTROL_DOWN); setOnAction { canvas.deleteSelected() } },
                SeparatorMenuItem(),
                MenuItem("모두 선택").apply { accelerator = KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN) }
            )
        }

        val insertMenu = Menu("_삽입").apply {
            items.addAll(
                MenuItem("노드").apply { setOnAction { addNode() } },
                MenuItem("엣지").apply { setOnAction { addEdge() } },
                MenuItem("Mermaid 블록").apply { setOnAction { addMermaidBlock() } },
                MenuItem("텍스트").apply { setOnAction { addText() } },
                MenuItem("PDF 요소...").apply { setOnAction { importPdfElement() } }
            )
        }

        val viewMenu = Menu("_보기").apply {
            items.addAll(
                CheckMenuItem("그리드 표시").apply { isSelected = document.gridEnabled; setOnAction { document.gridEnabled = isSelected; canvas.render() } },
                CheckMenuItem("눈금자 표시").apply { isSelected = document.showRulers; setOnAction { document.showRulers = isSelected; canvas.render() } },
                CheckMenuItem("스냅 사용").apply { isSelected = document.snapToGrid; setOnAction { document.snapToGrid = isSelected } },
                SeparatorMenuItem(),
                MenuItem("확대").apply { accelerator = KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN); setOnAction { zoomIn() } },
                MenuItem("축소").apply { accelerator = KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN); setOnAction { zoomOut() } },
                MenuItem("원래 크기").apply { accelerator = KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN); setOnAction { canvas.zoomReset() } },
                MenuItem("화면에 맞춤").apply { setOnAction { canvas.zoomToFit() } },
                SeparatorMenuItem(),
                Menu("테마").apply {
                    items.addAll(
                        RadioMenuItem("라이트"), RadioMenuItem("다크")
                    )
                }
            )
        }

        val toolsMenu = Menu("_도구").apply {
            items.addAll(
                MenuItem("PDF 편집기...").apply { setOnAction { openPdfEditor() } },
                MenuItem("Mermaid로 변환...").apply { setOnAction { showExportDialog() } },
                MenuItem("Mermaid 가져오기...").apply { setOnAction { showImportDialog() } },
                SeparatorMenuItem(),
                MenuItem("레이어 추가").apply { setOnAction { addLayer() } },
                MenuItem("선택한 레이어 삭제").apply { setOnAction { removeLayer() } }
            )
        }

        val helpMenu = Menu("_도움말").apply {
            items.addAll(
                MenuItem("FreeGram 정보").apply { setOnAction { Alert(Alert.AlertType.INFORMATION, "FreeGram v1.0\nDiagram Document Editor\nApache 2.0 License").showAndWait() } }
            )
        }

        return MenuBar(fileMenu, editMenu, insertMenu, viewMenu, toolsMenu, helpMenu)
    }

    private fun buildToolbar(): ToolBar {
        return ToolBar(
            Button("새 문서").apply { setOnAction { newDocument() } },
            Button("열기").apply { setOnAction { openDocument() } },
            Button("저장").apply { setOnAction { saveDocument() } },
            Separator(),
            Button("노드").apply { setOnAction { addNode() } },
            Button("엣지").apply { setOnAction { addEdge() } },
            Button("Mermaid").apply { setOnAction { addMermaidBlock() } },
            Button("텍스트").apply { setOnAction { addText() } },
            Button("PDF").apply { setOnAction { importPdfElement() } },
            Separator(),
            Button("실행취소").apply { setOnAction { undo() } },
            Button("재실행").apply { setOnAction { redo() } },
            Separator(),
            Button("PDF 편집").apply { setOnAction { openPdfEditor() } },
            Button("내보내기").apply { setOnAction { showExportDialog() } },
            Button("가져오기").apply { setOnAction { showImportDialog() } }
        )
    }

    private fun newDocument() {
        document.elements.clear(); document.resources.clear()
        document.title = "Untitled Diagram"
        document.createdAt = LocalDateTime.now(); document.modifiedAt = LocalDateTime.now()
        document.layers.clear(); document.layers.add(Layer("default", "기본 레이어"))
        currentFile = null
        canvas.setDocument(document)
        propertyPane.showElement(null, document)
        statusBar.text = "새 문서"
    }

    private fun openDocument() {
        val fileChooser = FileChooser().apply {
            title = "FDML 파일 열기"
            extensionFilters.add(FileChooser.ExtensionFilter("FDML files", "*.fdml", "*.xml"))
        }
        val file = fileChooser.showOpenDialog(stage) ?: return
        try {
            val content = file.readText()
            val doc = FdmlReader.read(content)
            document.elements.clear(); document.elements.addAll(doc.elements)
            document.resources.clear(); document.resources.putAll(doc.resources)
            document.title = doc.title; document.diagramWidth = doc.diagramWidth; document.diagramHeight = doc.diagramHeight
            document.layers = doc.layers
            currentFile = file
            canvas.setDocument(document)
            previewPane.updatePreview(document)
            statusBar.text = "열기: ${file.name}"
        } catch (e: Exception) {
            Alert(Alert.AlertType.ERROR, "파일 열기 실패: ${e.message}").showAndWait()
        }
    }

    private fun saveDocument(): Boolean {
        if (currentFile != null) return saveToFile(currentFile!!)
        return saveDocumentAs()
    }

    private fun saveDocumentAs(): Boolean {
        val fileChooser = FileChooser().apply {
            title = "FDML 파일 저장"
            extensionFilters.add(FileChooser.ExtensionFilter("FDML files", "*.fdml"))
        }
        val file = fileChooser.showSaveDialog(stage) ?: return false
        currentFile = file
        return saveToFile(file)
    }

    private fun saveToFile(file: File): Boolean {
        return try {
            document.modifiedAt = LocalDateTime.now()
            file.writeText(FdmlWriter.write(document))
            statusBar.text = "저장: ${file.name}"
            true
        } catch (e: Exception) {
            Alert(Alert.AlertType.ERROR, "저장 실패: ${e.message}").showAndWait()
            false
        }
    }

    private fun addNode() {
        val id = "node_${UUID.randomUUID().toString().take(8)}"
        val count = document.elements.filterIsInstance<DiagramElement.Node>().size
        val shapes = listOf(ShapeTypes.RECTANGLE, ShapeTypes.ROUND_RECT, ShapeTypes.DIAMOND, ShapeTypes.CIRCLE, ShapeTypes.PARALLELOGRAM, ShapeTypes.STAR, ShapeTypes.CYLINDER, ShapeTypes.NOTE, ShapeTypes.PROCESS)
        val node = DiagramElement.Node(
            id = id,
            position = Position(100.0 + (count % 5) * 150.0, 100.0 + (count / 5) * 120.0),
            size = Size(140.0, 60.0),
            content = "노드 ${count + 1}",
            style = Style(fillColor = "#4CAF50", strokeColor = "#2E7D32", shapeType = shapes[count % shapes.size]),
            layerId = document.activeLayerId
        )
        document.elements.add(node)
        captureUndo()
        canvas.render()
        statusBar.text = "노드 추가: $id"
    }

    private fun addEdge() {
        val nodes = document.elements.filterIsInstance<DiagramElement.Node>()
        if (nodes.size < 2) {
            Alert(Alert.AlertType.WARNING, "엣지를 만들려면 최소 2개의 노드가 필요합니다").showAndWait()
            return
        }
        val source = nodes[nodes.size - 2]; val target = nodes.last()
        val id = "edge_${UUID.randomUUID().toString().take(8)}"
        val edge = DiagramElement.Edge(
            id = id,
            position = Position((source.position.x + source.size.width + target.position.x) / 2, (source.position.y + target.position.y) / 2),
            sourceId = source.id, targetId = target.id,
            label = "edge", edgeType = "arrow",
            style = Style(strokeColor = "#333333", strokeWidth = 1.5,
                edgeDecoration = EdgeDecoration(targetArrow = EdgeArrowType.TRIANGLE)),
            layerId = document.activeLayerId
        )
        document.elements.add(edge)
        captureUndo()
        canvas.render()
        statusBar.text = "엣지 추가: $id"
    }

    private fun addMermaidBlock() {
        val id = "mermaid_${UUID.randomUUID().toString().take(8)}"
        val count = document.elements.filterIsInstance<DiagramElement.MermaidBlock>().size
        val block = DiagramElement.MermaidBlock(
            id = id,
            position = Position(100.0 + (count % 3) * 250.0, 100.0 + (count / 3) * 200.0),
            mermaidCode = "graph TD\n    A[Start] --> B[End]",
            width = 400.0, height = 300.0, isNativeMermaid = true,
            layerId = document.activeLayerId
        )
        document.elements.add(block)
        captureUndo()
        canvas.render()
        previewPane.updatePreview(document)
        statusBar.text = "Mermaid 블록 추가: $id"
    }

    private fun addText() {
        val id = "text_${UUID.randomUUID().toString().take(8)}"
        val text = DiagramElement.Text(
            id = id, position = Position(200.0, 200.0),
            text = "자유 텍스트", fontSize = 14.0,
            style = Style(fontColor = "#333333"),
            layerId = document.activeLayerId
        )
        document.elements.add(text)
        captureUndo()
        canvas.render()
        statusBar.text = "텍스트 추가: $id"
    }

    private fun importPdfElement() {
        val fileChooser = FileChooser().apply {
            title = "PDF 파일 선택"
            extensionFilters.add(FileChooser.ExtensionFilter("PDF files", "*.pdf"))
        }
        val file = fileChooser.showOpenDialog(stage) ?: return
        try {
            val pdfData = file.readBytes()
            val pdfDoc = org.apache.pdfbox.Loader.loadPDF(pdfData)
            for (i in 0 until pdfDoc.numberOfPages) {
                val page = pdfDoc.getPage(i)
                val id = "pdf_${UUID.randomUUID().toString().take(8)}"
                document.elements.add(DiagramElement.PdfElement(
                    id = id, position = Position(100.0, 100.0 + i * 30.0),
                    pdfData = pdfData, pageNumber = i + 1,
                    width = page.mediaBox.width.toDouble(), height = page.mediaBox.height.toDouble(),
                    layerId = document.activeLayerId
                ))
            }
            pdfDoc.close()
            captureUndo()
            canvas.render()
            statusBar.text = "PDF 가져오기 완료: ${pdfDoc.numberOfPages}페이지"
        } catch (e: Exception) {
            Alert(Alert.AlertType.ERROR, "PDF 가져오기 실패: ${e.message}").showAndWait()
        }
    }

    private fun undo() { if (undoManager.undo(document)) { canvas.render(); propertyPane.showElement(null, document); statusBar.text = "실행취소" } }
    private fun redo() { if (undoManager.redo(document)) { canvas.render(); propertyPane.showElement(null, document); statusBar.text = "재실행" } }
    private fun zoomIn() { canvas.zoomIn(); zoomLabel.text = "${(canvas.getDocument().zoomLevel * 100).toInt()}%" }
    private fun zoomOut() { canvas.zoomOut(); zoomLabel.text = "${(canvas.getDocument().zoomLevel * 100).toInt()}%" }

    private fun addLayer() {
        val name = "레이어 ${document.layers.size}"
        document.addLayer(name)
        layerCombo.items.add(name)
        layerCombo.selectionModel.select(name)
        statusBar.text = "레이어 추가: $name"
    }

    private fun removeLayer() {
        val name = layerCombo.value
        if (name == "default") { Alert(Alert.AlertType.WARNING, "기본 레이어는 삭제할 수 없습니다").showAndWait(); return }
        val layer = document.layers.find { it.name == name } ?: return
        document.removeLayer(layer.id)
        layerCombo.items.remove(name)
        layerCombo.selectionModel.select("default")
        canvas.render()
        statusBar.text = "레이어 삭제: $name"
    }

    private fun openPdfEditor() {
        val pdfElements = document.elements.filterIsInstance<DiagramElement.PdfElement>().toMutableList()
        if (pdfElements.isEmpty()) {
            Alert(Alert.AlertType.INFORMATION, "PDF 요소가 없습니다. 먼저 PDF를 가져오세요.").showAndWait()
            return
        }
        pdfEditor.openPdfEditor(pdfElements) { canvas.render() }
    }

    private fun showExportDialog() { exportDialog.showConversionDialog(document) }

    private fun showImportDialog() {
        exportDialog.showImportDialog importLoop@{ content, binary, format ->
            try {
                when (format) {
                    Format.FDML -> {
                        val doc = FdmlReader.read(content)
                        document.elements.clear(); document.elements.addAll(doc.elements)
                        document.title = doc.title
                        canvas.setDocument(document); previewPane.updatePreview(document)
                        statusBar.text = "FDML 가져오기 완료"
                    }
                    Format.MERMAID -> {
                        val result = MermaidToFdmlConverter.convert(content)
                        val doc = result.fdmlDocument ?: return@importLoop
                        document.elements.clear(); document.elements.addAll(doc.elements)
                        document.title = "Imported Mermaid"
                        canvas.setDocument(document); previewPane.updatePreview(document)
                        statusBar.text = "Mermaid 가져오기 완료"
                    }
                    Format.PDF -> {
                        if (binary != null) {
                            val result = PdfToFdmlConverter.convert(binary)
                            val doc = result.fdmlDocument ?: return@importLoop
                            document.elements.clear(); document.elements.addAll(doc.elements)
                            document.title = "Imported PDF"
                            canvas.setDocument(document); previewPane.updatePreview(document)
                            statusBar.text = "PDF 가져오기 완료 (${binary.size} bytes)"
                        }
                    }
                }
                captureUndo()
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR, "가져오기 실패: ${e.message}").showAndWait()
            }
        }
    }
}
