package io.github.lmliam.microsmith.cli.support

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

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

internal fun sha256IfRegularFile(path: Path): String? {
    val normalized = path.toAbsolutePath().normalize()
    if (!Files.isRegularFile(normalized)) {
        return null
    }
    return sha256(normalized)
}

private fun ByteArray.toHexString(): String = joinToString(separator = "") { "%02x".format(it) }
