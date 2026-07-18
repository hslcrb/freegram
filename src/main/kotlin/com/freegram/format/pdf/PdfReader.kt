package com.freegram.format.pdf

import com.freegram.model.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import java.util.*

object PdfReader {

    @Throws(Exception::class)
    fun read(pdfData: ByteArray): FdmlDocument {
        val doc = FdmlDocument(
            title = "Imported PDF",
            mermaidCompatibility = MermaidCompatibility(MermaidCompatibilityLevel.NONE, listOf("PDF documents cannot be converted to Mermaid format"))
        )

        val pdfDoc = Loader.loadPDF(pdfData)
        val pageCount = pdfDoc.numberOfPages

        for (i in 0 until pageCount) {
            val page: PDPage = pdfDoc.getPage(i)
            val mediaBox: PDRectangle = page.mediaBox
            val pageWidth = mediaBox.width.toDouble()
            val pageHeight = mediaBox.height.toDouble()

            val pdfElement = DiagramElement.PdfElement(
                id = "pdf_${i}_${UUID.randomUUID().toString().take(8)}",
                position = Position(50.0 + i * 30.0, 50.0 + i * 30.0),
                pdfData = pdfData,
                pageNumber = i + 1,
                width = pageWidth,
                height = pageHeight
            )
            doc.elements.add(pdfElement)
        }

        pdfDoc.close()
        return doc
    }
}
