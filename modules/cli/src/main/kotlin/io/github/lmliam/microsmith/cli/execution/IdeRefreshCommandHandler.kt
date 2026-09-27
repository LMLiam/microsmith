package io.github.lmliam.microsmith.cli.execution

import io.github.lmliam.microsmith.cli.command.IdeRefreshCommand
import io.github.lmliam.microsmith.cli.diagnostics.CliFailureCode
import io.github.lmliam.microsmith.cli.ide.IdeHelperRefreshResult

internal class IdeRefreshCommandHandler(
    private val emitterFactory: CliDiagnosticEmitterFactory,
    private val ideRefreshRunner: (IdeRefreshCommand) -> IdeHelperRefreshResult,
) {
    fun execute(command: IdeRefreshCommand): Int {
        val emitter = emitterFactory.create(command.diagnosticsFormat, command.verbose)
        val result =
            runCatching {
                ideRefreshRunner(command)
            }.getOrElse { error ->
                emitter.error(
                    CliFailureCode.IDE_HELPER_FAILED,
                    error.message ?: "JetBrains IDE helper generation failed.",
                )
                return CliFailureCode.IDE_HELPER_FAILED.exitCode
            }

        val helperRoot = result.helperRoot.toAbsolutePath().normalize()
        val refreshed = result.updatedFiles.size
        val state = if (refreshed == 0) "unchanged" else "updated"
        emitter.info(
            "JetBrains IDE helper is $state at '$helperRoot'.",
            details =
            mapOf(
                "projectRoot" to result.projectRoot.toAbsolutePath().normalize().toString(),
                "helperRoot" to helperRoot.toString(),
                "updatedFiles" to refreshed.toString(),
                "classpathEntries" to result.classpathEntries.size.toString(),
            ),
        )
        emitter.info("Import '${helperRoot.resolve("build.gradle.kts")}' as a Gradle project in JetBrains IDEs.")
        return 0
    }
}
