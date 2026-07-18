package com.freegram.model

enum class MermaidCompatibilityLevel {
    FULL,
    PARTIAL,
    NONE
}

data class MermaidCompatibility(
    val level: MermaidCompatibilityLevel,
    val reasons: List<String> = emptyList()
)
