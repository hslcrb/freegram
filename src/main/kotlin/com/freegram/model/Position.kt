package com.freegram.model

data class Position(val x: Double, val y: Double) {
    companion object {
        val ZERO = Position(0.0, 0.0)
    }
}

data class Size(val width: Double, val height: Double) {
    companion object {
        val ZERO = Size(0.0, 0.0)
    }
}

data class Bounds(val x: Double, val y: Double, val width: Double, val height: Double) {
    val position: Position get() = Position(x, y)
    val size: Size get() = Size(width, height)
}
