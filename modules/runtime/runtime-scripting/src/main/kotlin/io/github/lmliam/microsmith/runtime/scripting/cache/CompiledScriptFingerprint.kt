package io.github.lmliam.microsmith.runtime.scripting.cache

import dev.zacsweers.metro.Inject
import java.security.MessageDigest
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode

private const val COMPILED_SCRIPT_CACHE_VERSION = 3

@Inject
internal class CompiledScriptFingerprint(private val runtimeFingerprint: RuntimeClasspathFingerprint) {
    fun uniqueName(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        additionalFingerprints: List<String> = emptyList(),
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(COMPILED_SCRIPT_CACHE_VERSION.toByteArray())
        digest.addChunk(script.text)
        digest.addChunk(runtimeFingerprint.host)
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
