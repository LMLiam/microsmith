package me.liam.microsmith.runtime.scripting

import kotlin.script.experimental.jvm.util.classpathFromClassloader
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

internal object RuntimeClasspathFingerprint {
    private val runtimeFingerprint: String by lazy {
        val classpathEntries =
            classpathFromClassloader(
                MicrosmithScript::class.java.classLoader,
                unpackJarCollections = true
            ).orEmpty()
                .map { it.toPath().toAbsolutePath().normalize() }
        calculate(classpathEntries)
    }

    fun cached(): String = runtimeFingerprint

    fun calculate(classpathEntries: List<Path>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        classpathEntries
            .distinct()
            .sortedBy { it.toString() }
            .forEach { entry ->
                digest.addChunk(entry.toString())
                digest.addClasspathEntryFingerprint(entry)
            }
        return digest.digest().toHexString()
    }
}

private fun MessageDigest.addClasspathEntryFingerprint(entry: Path) {
    when {
        !Files.exists(entry) -> addChunk("missing")
        Files.isRegularFile(entry) -> {
            addChunk("file")
            addFileDigest(entry)
        }
        Files.isDirectory(entry) -> {
            addChunk("directory")
            addDirectoryFingerprint(entry)
        }
        else -> addChunk("other")
    }
}

private fun MessageDigest.addDirectoryFingerprint(directory: Path) {
    directoryRegularFiles(directory).forEach { file ->
        addChunk(directory.relativize(file).toString().replace('\\', '/'))
        addFileDigest(file)
    }
}

private fun directoryRegularFiles(directory: Path): List<Path> =
    Files.walk(directory).use { stream ->
        stream
            .filter { Files.isRegularFile(it) }
            .sorted(compareBy { it.toString() })
            .toList()
    }

private fun MessageDigest.addFileDigest(path: Path) {
    val fileDigest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) {
                break
            }
            fileDigest.update(buffer, 0, read)
        }
    }
    addChunk(fileDigest.digest().toHexString())
}

private fun MessageDigest.addChunk(chunk: String) {
    val chunkBytes = chunk.toByteArray()
    update(chunkBytes.size.toByteArray())
    update(chunkBytes)
}

private fun Int.toByteArray() =
    ByteBuffer.allocate(Int.SIZE_BYTES)
        .also { it.putInt(this) }
        .array()

private fun ByteArray.toHexString() = joinToString(separator = "") { "%02x".format(it) }
