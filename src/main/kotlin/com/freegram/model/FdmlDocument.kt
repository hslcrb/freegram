package com.freegram.model

import java.time.LocalDateTime
import java.util.UUID

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
    var gridSize: Double = 20.0,
    var layers: MutableList<Layer> = mutableListOf(Layer("default", "기본 레이어")),
    var activeLayerId: String = "default",
    var snapToGrid: Boolean = true,
    var showRulers: Boolean = true,
    var zoomLevel: Double = 1.0,
    var panX: Double = 0.0,
    var panY: Double = 0.0,
    var themeMode: String = "light"
) {
    fun getVisibleElements(): List<DiagramElement> {
        val visibleLayerIds = layers.filter { it.visible }.map { it.id }.toSet()
        return elements.filter { it.layerId in visibleLayerIds }
    }

    fun addLayer(name: String): Layer {
        val layer = Layer(
            id = UUID.randomUUID().toString().take(8),
            name = name,
            order = layers.size
        )
        layers.add(layer)
        return layer
    }

    fun removeLayer(layerId: String) {
        if (layerId == "default") return
        layers.removeAll { it.id == layerId }
        elements.filter { it.layerId == layerId }.forEach {
            val idx = elements.indexOf(it)
            if (idx >= 0) {
                val moved = when (it) {
                    is DiagramElement.Node -> it.copy(layerId = "default")
                    is DiagramElement.Edge -> it.copy(layerId = "default")
                    is DiagramElement.Text -> it.copy(layerId = "default")
                    is DiagramElement.MermaidBlock -> it.copy(layerId = "default")
                    is DiagramElement.PdfElement -> it.copy(layerId = "default")
                    is DiagramElement.Group -> it.copy(layerId = "default")
                }
                elements[idx] = moved
            }
        }
    }
}
