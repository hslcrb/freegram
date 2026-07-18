package com.freegram.converter

import com.freegram.format.pdf.PdfReader
import com.freegram.model.*

object PdfToFdmlConverter {

    @Throws(Exception::class)
    fun convert(pdfData: ByteArray): ConversionResult {
        val doc = PdfReader.read(pdfData)
        return ConversionResult(
            output = "",
            fdmlDocument = doc,
            compatibility = MermaidCompatibility(
                MermaidCompatibilityLevel.NONE,
                listOf("PDF content cannot be converted to Mermaid. PDF is embedded as pdf elements in FDML.")
            )
        )
    }
}
