package io.github.lmliam.microsmith.runtime.scripting.cache

import java.nio.ByteBuffer
import java.security.MessageDigest

internal fun MessageDigest.addChunk(chunk: String) {
    val chunkBytes = chunk.toByteArray()
    update(chunkBytes.size.toByteArray())
    update(chunkBytes)
}

internal fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this).array()
