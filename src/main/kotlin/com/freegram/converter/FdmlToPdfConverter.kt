package com.freegram.converter

import com.freegram.format.pdf.PdfWriter
import com.freegram.model.*

object FdmlToPdfConverter {

    @Throws(Exception::class)
    fun convert(doc: FdmlDocument): ConversionResult {
        val pdfData = PdfWriter.write(doc)
        return ConversionResult(
            output = "",  // binary PDF
            pdfData = pdfData,
            compatibility = MermaidCompatibility(MermaidCompatibilityLevel.FULL)
        )
    }
}
