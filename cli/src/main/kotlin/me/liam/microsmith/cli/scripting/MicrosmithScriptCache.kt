package me.liam.microsmith.cli.scripting

import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache
import java.nio.ByteBuffer
import java.nio.file.Path
import java.security.MessageDigest

private const val COMPILED_SCRIPT_CACHE_VERSION = 1

internal class MicrosmithScriptCache(
    private val cacheDirectory: Path
) : CompiledScriptJarsCache(
        { script, scriptCompilationConfiguration ->
            cacheDirectory.resolve(compiledScriptUniqueName(script, scriptCompilationConfiguration) + ".jar").toFile()
        }
    ) {
    var storedScripts: Int = 0
        private set

    var retrievedScripts: Int = 0
        private set

    override fun get(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): CompiledScript? =
        super.get(script, scriptCompilationConfiguration)?.also { retrievedScripts++ }

    override fun store(
        compiledScript: CompiledScript,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ) {
        super.store(compiledScript, script, scriptCompilationConfiguration)
        storedScripts++
    }
}

private fun compiledScriptUniqueName(
    script: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration
): String {
    val digest = MessageDigest.getInstance("SHA-256")

    fun addChunk(chunk: String) {
        val chunkBytes = chunk.toByteArray()
        digest.update(chunkBytes.size.toByteArray())
        digest.update(chunkBytes)
    }

    digest.update(COMPILED_SCRIPT_CACHE_VERSION.toByteArray())
    addChunk(script.text)
    scriptCompilationConfiguration.notTransientData.entries
        .sortedBy { it.key.name }
        .forEach {
            addChunk(it.key.name)
            addChunk(it.value.toString())
        }
    return digest.digest().toHexString()
}

private fun Int.toByteArray() =
    ByteBuffer.allocate(Int.SIZE_BYTES)
        .also { it.putInt(this) }
        .array()

private fun ByteArray.toHexString() = joinToString(separator = "") { "%02x".format(it) }
