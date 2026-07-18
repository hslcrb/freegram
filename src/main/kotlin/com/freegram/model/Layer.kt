package com.freegram.model

data class Layer(
    val id: String,
    var name: String,
    var visible: Boolean = true,
    var locked: Boolean = false,
    var order: Int = 0
)
