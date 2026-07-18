package com.freegram.model

data class ClipboardState(
    val elements: List<DiagramElement> = emptyList(),
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0
)
