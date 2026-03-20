package io.github.lmliam.microsmith.build.runtime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class RuntimeScriptingSourceFilesTests : StringSpec({
    "discovers runtime scripting definition files from source contents" {
        val sourceRoot = tempSourceRoot()
        writeSource(
            sourceRoot,
            "io/github/lmliam/microsmith/runtime/scripting/definition/MicrosmithScript.kt",
            """
            package io.github.lmliam.microsmith.runtime.scripting.definition

            import kotlin.script.experimental.annotations.KotlinScript

            @KotlinScript(
                fileExtension = "microscript.kts",
                compilationConfiguration = MicrosmithScriptCompilationConfiguration::class,
            )
            @Suppress("UnnecessaryAbstractClass")
            abstract class MicrosmithScript
            """.trimIndent(),
        )
        writeSource(
            sourceRoot,
            "io/github/lmliam/microsmith/runtime/scripting/definition/MicrosmithScriptCompilationConfiguration.kt",
            """
            package io.github.lmliam.microsmith.runtime.scripting.definition

            import kotlin.script.experimental.api.ScriptCompilationConfiguration
            import kotlin.script.experimental.api.implicitReceivers

            object MicrosmithScriptCompilationConfiguration : ScriptCompilationConfiguration({
                implicitReceivers(MicrosmithScriptContext::class)
            })
            """.trimIndent(),
        )
        writeSource(
            sourceRoot,
            "io/github/lmliam/microsmith/runtime/scripting/context/MicrosmithScriptContext.kt",
            """
            package io.github.lmliam.microsmith.runtime.scripting.context

            class MicrosmithScriptContext
            """.trimIndent(),
        )
        writeSource(
            sourceRoot,
            "io/github/lmliam/microsmith/runtime/scripting/host/Helpers.kt",
            """
            package io.github.lmliam.microsmith.runtime.scripting.host

            fun helper() = Unit
            """.trimIndent(),
        )

        val scriptTemplateSourceFile =
            RuntimeScriptingSourceFiles.annotatedKotlinScriptSourceFile(sourceRoot.toFile(), "microscript.kts")
        scriptTemplateSourceFile.name shouldBe "MicrosmithScript.kt"
        RuntimeScriptingSourceFiles.fqcnFromSourceFile(scriptTemplateSourceFile) shouldBe
            "io.github.lmliam.microsmith.runtime.scripting.definition.MicrosmithScript"

        val compilationConfigurationSourceFile =
            RuntimeScriptingSourceFiles.compilationConfigurationSourceFileFromScriptTemplateSourceFile(
                sourceRoot.toFile(),
                scriptTemplateSourceFile,
            )
        RuntimeScriptingSourceFiles.fqcnFromSourceFile(compilationConfigurationSourceFile) shouldBe
            "io.github.lmliam.microsmith.runtime.scripting.definition.MicrosmithScriptCompilationConfiguration"

        val contextSourceFile =
            RuntimeScriptingSourceFiles.contextSourceFileFromCompilationConfigurationSourceFile(
                sourceRoot.toFile(),
                compilationConfigurationSourceFile,
            )
        RuntimeScriptingSourceFiles.fqcnFromSourceFile(contextSourceFile) shouldBe
            "io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext"
    }
})

private fun tempSourceRoot(): Path = Files.createTempDirectory("runtime-scripting-source-files")
    .resolve("src/main/kotlin")
    .also { Files.createDirectories(it) }

private fun writeSource(sourceRoot: Path, relativePath: String, contents: String) {
    val file = sourceRoot.resolve(relativePath)
    Files.createDirectories(file.parent)
    Files.writeString(file, "$contents\n", StandardCharsets.UTF_8)
}
