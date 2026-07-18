package com.freegram.model

import java.time.LocalDateTime

data class FdmlDocument(
    var version: String = "1.0",
    var title: String = "Untitled Diagram",
    var author: String = "",
    var description: String = "",
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var modifiedAt: LocalDateTime = LocalDateTime.now(),
    var mermaidCompatibility: MermaidCompatibility = MermaidCompatibility(MermaidCompatibilityLevel.FULL),
    var resources: MutableMap<String, EmbeddedResource> = mutableMapOf(),
    var elements: MutableList<DiagramElement> = mutableListOf(),
    var diagramWidth: Double = 1200.0,
    var diagramHeight: Double = 900.0,
    var backgroundColor: String = "#FFFFFF",
    var gridEnabled: Boolean = true,
    var gridSize: Double = 20.0
)
