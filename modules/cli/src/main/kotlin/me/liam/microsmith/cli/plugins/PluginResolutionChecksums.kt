package me.liam.microsmith.cli.plugins

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

private const val HEX_SHA256_LENGTH = 64

internal fun sha256(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) {
                break
            }
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().toHexString()
}

internal fun isSha256(value: String): Boolean = value.length == HEX_SHA256_LENGTH && value.matches(Regex("^[a-f0-9]+$"))

private fun ByteArray.toHexString(): String = joinToString(separator = "") { "%02x".format(it) }
