package me.liam.microsmith.cli.init

import me.liam.microsmith.cli.command.IdeRefreshCommand
import me.liam.microsmith.cli.command.InitCommand
import me.liam.microsmith.cli.ide.IdeHelperConflictException
import me.liam.microsmith.cli.ide.IdeHelperRefreshResult
import me.liam.microsmith.cli.ide.refreshIdeHelperProject
import java.nio.file.Files
import java.nio.file.Path

internal class InitBootstrapper(
    private val command: InitCommand,
    private val ideRefreshRunner: (IdeRefreshCommand) -> IdeHelperRefreshResult = ::refreshIdeHelperProject,
    private val fileWriter: BootstrapFileWriter = BootstrapFileWriter(),
) {
    fun run(): InitBootstrapResult {
        val projectRoot = command.projectRoot.toAbsolutePath().normalize()
        validateProjectRoot(projectRoot)

        val repositoryDetection = detectOnboardingRepositoryType(projectRoot)
        val createdFiles = mutableListOf<Path>()
        val overwrittenFiles = mutableListOf<Path>()
        val preservedFiles = mutableListOf<Path>()

        BootstrapScriptTemplates.filesFor(repositoryDetection).forEach { (relativePath, content) ->
            val fileWriteResult =
                fileWriter.write(
                    path = projectRoot.resolve(relativePath),
                    content = content,
                    force = command.force,
                )
            when (fileWriteResult) {
                is BootstrapFileWriteResult.Created -> createdFiles.add(fileWriteResult.path)
                is BootstrapFileWriteResult.Overwritten -> overwrittenFiles.add(fileWriteResult.path)
                is BootstrapFileWriteResult.Preserved -> preservedFiles.add(fileWriteResult.path)
            }
        }

        val ideHelperResult = refreshIdeHelperIfEnabled(projectRoot)

        return InitBootstrapResult(
            projectRoot = projectRoot,
            repositoryDetection = repositoryDetection,
            createdFiles = createdFiles.sortedBy(Path::toString),
            overwrittenFiles = overwrittenFiles.sortedBy(Path::toString),
            preservedFiles = preservedFiles.sortedBy(Path::toString),
            ideHelperResult = ideHelperResult,
        )
    }

    private fun refreshIdeHelperIfEnabled(projectRoot: Path): IdeHelperRefreshResult? {
        if (command.skipIdeHelper) {
            return null
        }

        return runCatching {
            ideRefreshRunner(
                IdeRefreshCommand(
                    projectRoot = projectRoot,
                    diagnosticsFormat = command.diagnosticsFormat,
                    verbose = command.verbose,
                ),
            )
        }.getOrElse { error ->
            when (error) {
                is IdeHelperConflictException ->
                    throw InitConflictException(error.message ?: "IDE helper path is invalid.")

                else -> throw error
            }
        }
    }

    private fun validateProjectRoot(projectRoot: Path) {
        requireInit(Files.exists(projectRoot)) {
            "Repository root '$projectRoot' does not exist."
        }
        requireInit(Files.isDirectory(projectRoot)) {
            "Repository root '$projectRoot' is not a directory."
        }
    }
}

internal fun runInitBootstrap(
    command: InitCommand,
    ideRefreshRunner: (IdeRefreshCommand) -> IdeHelperRefreshResult = ::refreshIdeHelperProject,
): InitBootstrapResult = InitBootstrapper(command = command, ideRefreshRunner = ideRefreshRunner).run()

private inline fun requireInit(value: Boolean, lazyMessage: () -> String) {
    if (!value) {
        throw InitValidationException(lazyMessage())
    }
}
