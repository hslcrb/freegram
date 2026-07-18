package com.freegram.converter

import com.freegram.format.mermaid.MermaidReader
import com.freegram.model.*

object MermaidToFdmlConverter {

    fun convert(mermaidCode: String): ConversionResult {
        val doc = MermaidReader.read(mermaidCode)
        return ConversionResult(
            output = "",  // FDML requires write step
            fdmlDocument = doc,
            compatibility = MermaidCompatibility(MermaidCompatibilityLevel.FULL)
        )
    }
}
