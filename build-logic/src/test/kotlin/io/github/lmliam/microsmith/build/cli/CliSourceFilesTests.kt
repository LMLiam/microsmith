package io.github.lmliam.microsmith.build.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class CliSourceFilesTests : StringSpec({
    "discovers the CLI main source file from top-level function contents" {
        val sourceRoot = tempSourceRoot()
        writeSource(
            sourceRoot,
            "io/github/lmliam/microsmith/cli/Cli.kt",
            """
            package io.github.lmliam.microsmith.cli

            import kotlin.system.exitProcess

            fun main(args: Array<String>) {
                exitProcess(MicrosmithCli().run(args))
            }
            """.trimIndent(),
        )
        writeSource(
            sourceRoot,
            "io/github/lmliam/microsmith/cli/Helpers.kt",
            """
            package io.github.lmliam.microsmith.cli

            fun helper() = Unit
            """.trimIndent(),
        )

        val mainFunctionSourceFile = CliSourceFiles.mainFunctionSourceFile(sourceRoot.toFile())
        mainFunctionSourceFile.name shouldBe "Cli.kt"
        CliSourceFiles.mainClassNameFromSourceFile(mainFunctionSourceFile) shouldBe "CliKt"
        CliSourceFiles.applicationMainClassFromSourceRoot(sourceRoot.toFile()) shouldBe
            "io.github.lmliam.microsmith.cli.CliKt"
    }
})

private fun tempSourceRoot(): Path = Files.createTempDirectory("cli-source-files")
    .resolve("src/main/kotlin")
    .also { Files.createDirectories(it) }

private fun writeSource(sourceRoot: Path, relativePath: String, contents: String) {
    val file = sourceRoot.resolve(relativePath)
    Files.createDirectories(file.parent)
    Files.writeString(file, "$contents\n", StandardCharsets.UTF_8)
}
