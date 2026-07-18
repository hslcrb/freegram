package com.freegram.converter

import com.freegram.format.fdml.FdmlReader
import com.freegram.format.fdml.FdmlWriter
import com.freegram.format.mermaid.MermaidWriter
import com.freegram.format.pdf.PdfWriter
import com.freegram.model.FdmlDocument
import com.freegram.model.MermaidCompatibility
import com.freegram.model.MermaidCompatibilityLevel

enum class Format {
    FDML, MERMAID, PDF
}

data class ConversionPath(
    val from: Format,
    val to: Format
) {
    fun describe(): String = "$from → $to"
}

class ConversionManager {

    fun convert(
        source: String,
        sourceFormat: Format,
        targetFormat: Format,
        sourceBinary: ByteArray? = null
    ): ConversionResult {
        return when (sourceFormat to targetFormat) {
            Format.FDML to Format.MERMAID -> fdmlToMermaid(source)
            Format.FDML to Format.PDF -> fdmlToPdf(source)
            Format.MERMAID to Format.FDML -> mermaidToFdml(source)
            Format.MERMAID to Format.PDF -> mermaidToPdf(source)
            Format.PDF to Format.FDML -> if (sourceBinary != null) pdfToFdml(sourceBinary) else error("PDF requires binary data")
            Format.PDF to Format.MERMAID -> if (sourceBinary != null) pdfToMermaid(sourceBinary) else error("PDF requires binary data")
            Format.FDML to Format.FDML -> ConversionResult(output = source, compatibility = MermaidCompatibility(MermaidCompatibilityLevel.FULL))
            Format.MERMAID to Format.MERMAID -> ConversionResult(output = source, compatibility = MermaidCompatibility(MermaidCompatibilityLevel.FULL))
            Format.PDF to Format.PDF -> ConversionResult(pdfData = sourceBinary, compatibility = MermaidCompatibility(MermaidCompatibilityLevel.FULL))
            else -> error("Unsupported conversion: $sourceFormat -> $targetFormat")
        }
    }

    private fun fdmlToMermaid(fdml: String): ConversionResult {
        val doc = FdmlReader.read(fdml)
        val result = FdmlToMermaidConverter.convert(doc)
        return result
    }

    private fun fdmlToPdf(fdml: String): ConversionResult {
        val doc = FdmlReader.read(fdml)
        return try {
            val pdf = PdfWriter.write(doc)
            ConversionResult(pdfData = pdf, compatibility = MermaidCompatibility(MermaidCompatibilityLevel.FULL))
        } catch (e: Exception) {
            ConversionResult(
                compatibility = MermaidCompatibility(MermaidCompatibilityLevel.NONE, listOf("PDF generation failed: ${e.message}"))
            )
        }
    }

    private fun mermaidToFdml(mermaid: String): ConversionResult {
        val result = MermaidToFdmlConverter.convert(mermaid)
        val fdml = result.fdmlDocument?.let { FdmlWriter.write(it) } ?: ""
        return ConversionResult(output = fdml, fdmlDocument = result.fdmlDocument, compatibility = result.compatibility)
    }

    private fun mermaidToPdf(mermaid: String): ConversionResult {
        val fdmlResult = mermaidToFdml(mermaid)
        val doc = fdmlResult.fdmlDocument ?: return ConversionResult(
            compatibility = MermaidCompatibility(MermaidCompatibilityLevel.NONE, listOf("Failed to parse Mermaid"))
        )
        return try {
            val pdf = PdfWriter.write(doc)
            ConversionResult(pdfData = pdf, compatibility = fdmlResult.compatibility)
        } catch (e: Exception) {
            ConversionResult(
                compatibility = MermaidCompatibility(MermaidCompatibilityLevel.NONE, listOf("PDF generation failed: ${e.message}"))
            )
        }
    }

    private fun pdfToFdml(pdfData: ByteArray): ConversionResult {
        val result = PdfToFdmlConverter.convert(pdfData)
        val fdml = result.fdmlDocument?.let { FdmlWriter.write(it) } ?: ""
        return ConversionResult(output = fdml, fdmlDocument = result.fdmlDocument, compatibility = result.compatibility)
    }

    private fun pdfToMermaid(pdfData: ByteArray): ConversionResult {
        val fdmlResult = pdfToFdml(pdfData)
        val doc = fdmlResult.fdmlDocument ?: return ConversionResult(
            compatibility = MermaidCompatibility(MermaidCompatibilityLevel.NONE, listOf("Failed to parse PDF"))
        )
        val mermaid = FdmlToMermaidConverter.convert(doc)
        return mermaid
    }
}
