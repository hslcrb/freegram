package com.freegram.converter

import com.freegram.model.FdmlDocument
import com.freegram.model.MermaidCompatibility

data class ConversionResult(
    val output: String = "",
    val fdmlDocument: FdmlDocument? = null,
    val pdfData: ByteArray? = null,
    val compatibility: MermaidCompatibility = MermaidCompatibility(com.freegram.model.MermaidCompatibilityLevel.FULL)
)
