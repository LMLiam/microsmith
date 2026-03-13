package io.github.lmliam.microsmith.runtime.scripting.cache

import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode

private const val COMPILED_SCRIPT_CACHE_VERSION = 1

internal object CompiledScriptFingerprint {
    fun uniqueName(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        additionalFingerprints: List<String> = emptyList(),
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(COMPILED_SCRIPT_CACHE_VERSION.toByteArray())
        digest.addChunk(script.text)
        digest.addChunk(RuntimeClasspathFingerprint.cached())
        scriptCompilationConfiguration.notTransientData.entries
            .sortedBy { it.key.name }
            .forEach { entry ->
                digest.addChunk(entry.key.name)
                digest.addChunk(entry.value.toString())
            }
        additionalFingerprints.sorted().forEach(digest::addChunk)
        return digest.digest().toHexString()
    }
}

private fun MessageDigest.addChunk(chunk: String) {
    val chunkBytes = chunk.toByteArray()
    update(chunkBytes.size.toByteArray())
    update(chunkBytes)
}

private fun Int.toByteArray() = ByteBuffer.allocate(Int.SIZE_BYTES)
    .also { it.putInt(this) }
    .array()

private fun ByteArray.toHexString() = joinToString(separator = "") { "%02x".format(it) }
