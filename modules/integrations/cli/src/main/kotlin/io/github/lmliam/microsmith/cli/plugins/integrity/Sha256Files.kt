package io.github.lmliam.microsmith.cli.plugins.integrity

import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile

internal fun sha256(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    path.inputStream().use { input ->
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
    if (!normalized.isRegularFile()) {
        return null
    }
    return sha256(normalized)
}
