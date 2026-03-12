package me.liam.microsmith.runtime.scripting.cache

import java.nio.file.Path
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache

internal class MicrosmithScriptCache(
    private val cacheDirectory: Path,
    private val additionalFingerprints: () -> List<String> = { emptyList() },
) : CompiledScriptJarsCache(
    { script, scriptCompilationConfiguration ->
        cacheDirectory
            .resolve(
                CompiledScriptFingerprint.uniqueName(
                    script = script,
                    scriptCompilationConfiguration = scriptCompilationConfiguration,
                    additionalFingerprints = additionalFingerprints(),
                ) + ".jar",
            )
            .toFile()
    },
) {
    var storedScripts: Int = 0
        private set

    var retrievedScripts: Int = 0
        private set

    override fun get(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
    ): CompiledScript? = super.get(script, scriptCompilationConfiguration)?.also { retrievedScripts++ }

    override fun store(
        compiledScript: CompiledScript,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
    ) {
        super.store(compiledScript, script, scriptCompilationConfiguration)
        storedScripts++
    }
}
