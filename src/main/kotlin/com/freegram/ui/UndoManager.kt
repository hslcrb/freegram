package com.freegram.ui

import com.freegram.model.*

class UndoManager(private val maxHistory: Int = 50) {
    private val undoStack = mutableListOf<Snapshot>()
    private val redoStack = mutableListOf<Snapshot>()

    data class Snapshot(
        val elements: List<DiagramElement>,
        val layers: List<Layer>,
        val title: String
    )

    fun capture(doc: FdmlDocument) {
        undoStack.add(Snapshot(
            elements = doc.elements.map { it }, // shallow copy list
            layers = doc.layers.map { it.copy() },
            title = doc.title
        ))
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(doc: FdmlDocument): Boolean {
        if (undoStack.isEmpty()) return false
        val current = Snapshot(
            elements = doc.elements.toList(),
            layers = doc.layers.map { it.copy() },
            title = doc.title
        )
        redoStack.add(current)
        val prev = undoStack.removeLast()
        restore(doc, prev)
        return true
    }

    fun redo(doc: FdmlDocument): Boolean {
        if (redoStack.isEmpty()) return false
        val current = Snapshot(
            elements = doc.elements.toList(),
            layers = doc.layers.map { it.copy() },
            title = doc.title
        )
        undoStack.add(current)
        val next = redoStack.removeLast()
        restore(doc, next)
        return true
    }

    private fun restore(doc: FdmlDocument, snap: Snapshot) {
        doc.elements.clear()
        doc.elements.addAll(snap.elements)
        doc.layers.clear()
        doc.layers.addAll(snap.layers)
        doc.title = snap.title
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size
}
