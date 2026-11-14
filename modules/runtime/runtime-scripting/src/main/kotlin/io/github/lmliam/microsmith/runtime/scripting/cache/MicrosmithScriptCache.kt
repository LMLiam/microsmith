package io.github.lmliam.microsmith.runtime.scripting.cache

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.encoding.Base64
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.jvmhost.CompiledScriptJarsCache

internal class MicrosmithScriptCache(
    private val cacheDirectory: Path,
    private val additionalFingerprints: () -> List<String> = { emptyList() },
) :
    CompiledScriptJarsCache(
        { script, scriptCompilationConfiguration ->
            compiledJar(cacheDirectory, script, scriptCompilationConfiguration, additionalFingerprints())
        },
    ) {
    var storedScripts: Int = 0
        private set

    var retrievedScripts: Int = 0
        private set

    var replayedWarnings: List<String> = emptyList()
        private set

    private var storedJar: File? = null

    override fun get(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
    ): CompiledScript? {
        val warnings = readWarnings(jarOf(script, scriptCompilationConfiguration)) ?: return null
        return super.get(script, scriptCompilationConfiguration)?.also {
            retrievedScripts++
            replayedWarnings = warnings
        }
    }

    override fun store(
        compiledScript: CompiledScript,
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
    ) {
        val jar = jarOf(script, scriptCompilationConfiguration)
        jar.warningsFile().delete()
        super.store(compiledScript, script, scriptCompilationConfiguration)
        storedScripts++
        storedJar = jar
    }

    fun recordWarnings(warnings: List<String>) {
        storedJar?.let { jar -> writeWarnings(jar.warningsFile(), warnings) }
    }

    private fun jarOf(script: SourceCode, scriptCompilationConfiguration: ScriptCompilationConfiguration): File =
        compiledJar(cacheDirectory, script, scriptCompilationConfiguration, additionalFingerprints())

    private fun File.warningsFile(): File = resolveSibling("$nameWithoutExtension.warnings")

    private fun readWarnings(jar: File): List<String>? = runCatching {
        jar.warningsFile().takeIf(File::isFile)?.readLines()?.map { line -> Base64.decode(line).decodeToString() }
    }
        .getOrNull()

    private fun writeWarnings(file: File, warnings: List<String>) {
        val temporary = Files.createTempFile(file.parentFile.toPath(), file.name, ".tmp")
        try {
            Files.write(
                temporary,
                warnings.map { warning -> Base64.encode(warning.encodeToByteArray()) },
            )
            Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}

private fun compiledJar(
    cacheDirectory: Path,
    script: SourceCode,
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
    additionalFingerprints: List<String>,
): File =
    cacheDirectory
        .resolve(
            CompiledScriptFingerprint.uniqueName(
                script = script,
                scriptCompilationConfiguration = scriptCompilationConfiguration,
                additionalFingerprints = additionalFingerprints,
            ) + ".jar",
        )
        .toFile()
