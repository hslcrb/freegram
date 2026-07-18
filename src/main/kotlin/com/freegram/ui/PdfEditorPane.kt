package com.freegram.ui

import com.freegram.model.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.io.ByteArrayOutputStream
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.FileChooser
import javafx.stage.Modality
import javafx.stage.Stage
import java.io.File
import java.util.*

class PdfEditorPane(private val owner: Stage) {

    fun openPdfEditor(pdfElements: MutableList<DiagramElement.PdfElement>, onUpdate: () -> Unit) {
        val dialog = Stage()
        dialog.initModality(Modality.WINDOW_MODAL)
        dialog.initOwner(owner)
        dialog.title = "FreeGram - PDF Editor"

        val root = VBox(12.0)
        root.padding = Insets(16.0)

        val pageList = ListView<String>()
        pageList.prefHeight = 300.0
        refreshList(pdfElements, pageList)

        val mergeBtn = Button("PDF 병합...")
        val extractBtn = Button("선택 페이지 추출...")
        val reorderUpBtn = Button("▲ 위로")
        val reorderDownBtn = Button("▼ 아래로")
        val removeBtn = Button("삭제")
        val addAnnotationBtn = Button("주석 페이지 추가")
        val closeBtn = Button("닫기")

        mergeBtn.setOnAction {
            val fileChooser = FileChooser().apply {
                title = "병합할 PDF 선택"
                extensionFilters.add(FileChooser.ExtensionFilter("PDF files", "*.pdf"))
            }
            val file = fileChooser.showOpenDialog(dialog) ?: return@setOnAction
            try {
                val newPdfData = file.readBytes()
                val newDoc = Loader.loadPDF(newPdfData)
                for (i in 0 until newDoc.numberOfPages) {
                    pdfElements.add(DiagramElement.PdfElement(
                        id = "pdf_${UUID.randomUUID().toString().take(8)}",
                        position = Position(50.0 + pdfElements.size * 30.0, 50.0 + pdfElements.size * 30.0),
                        pdfData = newPdfData,
                        pageNumber = i + 1,
                        width = newDoc.getPage(i).mediaBox.width.toDouble(),
                        height = newDoc.getPage(i).mediaBox.height.toDouble()
                    ))
                }
                newDoc.close()
                refreshList(pdfElements, pageList)
                onUpdate()
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR, "PDF 병합 실패: ${e.message}").showAndWait()
            }
        }

        extractBtn.setOnAction {
            val selected = pageList.selectionModel.selectedIndex
            if (selected < 0 || selected >= pdfElements.size) return@setOnAction
            val element = pdfElements[selected]
            try {
                val extractedDoc = PDDocument()
                val sourceDoc = Loader.loadPDF(element.pdfData)
                extractedDoc.addPage(sourceDoc.getPage(element.pageNumber - 1))
                val baos = ByteArrayOutputStream()
                extractedDoc.save(baos)
                extractedDoc.close()
                sourceDoc.close()

                val fileChooser = FileChooser().apply {
                    title = "추출한 페이지 저장"
                    extensionFilters.add(FileChooser.ExtensionFilter("PDF files", "*.pdf"))
                }
                val file = fileChooser.showSaveDialog(dialog) ?: return@setOnAction
                file.writeBytes(baos.toByteArray())
            } catch (e: Exception) {
                Alert(Alert.AlertType.ERROR, "추출 실패: ${e.message}").showAndWait()
            }
        }

        reorderUpBtn.setOnAction {
            val idx = pageList.selectionModel.selectedIndex
            if (idx > 0) {
                Collections.swap(pdfElements, idx, idx - 1)
                refreshList(pdfElements, pageList)
                pageList.selectionModel.select(idx - 1)
                onUpdate()
            }
        }

        reorderDownBtn.setOnAction {
            val idx = pageList.selectionModel.selectedIndex
            if (idx >= 0 && idx < pdfElements.size - 1) {
                Collections.swap(pdfElements, idx, idx + 1)
                refreshList(pdfElements, pageList)
                pageList.selectionModel.select(idx + 1)
                onUpdate()
            }
        }

        removeBtn.setOnAction {
            val idx = pageList.selectionModel.selectedIndex
            if (idx >= 0 && idx < pdfElements.size) {
                pdfElements.removeAt(idx)
                refreshList(pdfElements, pageList)
                onUpdate()
            }
        }

        addAnnotationBtn.setOnAction {
            val annotationPdf = createAnnotationPage()
            pdfElements.add(DiagramElement.PdfElement(
                id = "annot_${UUID.randomUUID().toString().take(8)}",
                position = Position(100.0, 100.0),
                pdfData = annotationPdf,
                pageNumber = 1,
                width = 595.0,
                height = 842.0
            ))
            refreshList(pdfElements, pageList)
            onUpdate()
        }

        closeBtn.setOnAction { dialog.close() }

        val btnRow = HBox(8.0, mergeBtn, extractBtn, reorderUpBtn, reorderDownBtn, removeBtn, addAnnotationBtn)

        root.children.addAll(
            Label("PDF 페이지 편집기 — ${pdfElements.size}개 페이지"),
            Separator(),
            pageList,
            btnRow,
            Separator(),
            closeBtn
        )

        dialog.scene = Scene(root, 700.0, 500.0)
        dialog.showAndWait()
    }

    private fun refreshList(elements: List<DiagramElement.PdfElement>, listView: ListView<String>) {
        listView.items.clear()
        elements.forEachIndexed { i, el ->
            listView.items.add("${i + 1}. ${el.id} (${el.width.toInt()}x${el.height.toInt()}, ${el.pdfData.size} bytes)")
        }
    }

    private fun createAnnotationPage(): ByteArray {
        try {
            val doc = PDDocument()
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            val cs = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)
            cs.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 24f)
            cs.beginText()
            cs.newLineAtOffset(100f, 700f)
            cs.showText("FreeGram Annotation")
            cs.endText()

            cs.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
            cs.beginText()
            cs.newLineAtOffset(100f, 650f)
            cs.showText("Created: ${java.time.LocalDateTime.now()}")
            cs.endText()
            cs.close()
            val baos = ByteArrayOutputStream()
            doc.save(baos)
            doc.close()
            return baos.toByteArray()
        } catch (_: Exception) {
            return ByteArray(0)
        }
    }
}
