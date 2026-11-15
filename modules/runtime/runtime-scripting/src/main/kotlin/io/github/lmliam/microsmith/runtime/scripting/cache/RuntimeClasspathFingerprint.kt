package io.github.lmliam.microsmith.runtime.scripting.cache

import io.github.lmliam.microsmith.runtime.scripting.definition.MicrosmithScript
import java.nio.ByteBuffer
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk
import kotlin.script.experimental.jvm.util.classpathFromClassloader

internal object RuntimeClasspathFingerprint {
    private val runtimeFingerprint: String by lazy {
        val classpathEntries =
            classpathFromClassloader(
                    MicrosmithScript::class.java.classLoader,
                    unpackJarCollections = true,
                )
                .orEmpty()
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
        !entry.exists() -> addChunk("missing")

        entry.isRegularFile() -> {
            addChunk("file")
            addFileDigest(entry)
        }

        entry.isDirectory() -> {
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

@OptIn(ExperimentalPathApi::class)
private fun directoryRegularFiles(directory: Path): List<Path> =
    directory.walk().filter { it.isRegularFile() }.sortedBy { it.toString() }.toList()

private fun MessageDigest.addFileDigest(path: Path) {
    val fileDigest = MessageDigest.getInstance("SHA-256")
    path.inputStream().use { input ->
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

private fun Int.toByteArray() = ByteBuffer.allocate(Int.SIZE_BYTES).also { it.putInt(this) }.array()
