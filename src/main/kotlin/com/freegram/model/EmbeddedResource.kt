package com.freegram.model

sealed class EmbeddedResource {
    abstract val id: String
    abstract val mimeType: String
    abstract val data: ByteArray

    data class Font(
        override val id: String,
        override val data: ByteArray,
        val fontFamily: String,
        val fontWeight: String = "normal",
        val fontStyle: String = "normal",
        override val mimeType: String = "font/ttf"
    ) : EmbeddedResource()

    data class Image(
        override val id: String,
        override val data: ByteArray,
        override val mimeType: String
    ) : EmbeddedResource()

    data class Raw(
        override val id: String,
        override val data: ByteArray,
        override val mimeType: String,
        val description: String = ""
    ) : EmbeddedResource()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddedResource) return false
        return id == other.id && mimeType == other.mimeType && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
