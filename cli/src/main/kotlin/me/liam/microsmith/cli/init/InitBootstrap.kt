package me.liam.microsmith.cli.init

import me.liam.microsmith.cli.command.IdeRefreshCommand
import me.liam.microsmith.cli.command.InitCommand
import me.liam.microsmith.cli.ide.IdeHelperRefreshResult
import me.liam.microsmith.cli.ide.refreshIdeHelperProject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

internal data class InitBootstrapResult(
    val projectRoot: Path,
    val createdFiles: List<Path>,
    val preservedFiles: List<Path>,
    val ideHelperResult: IdeHelperRefreshResult,
)

internal class InitConflictException(message: String) : IllegalStateException(message)

internal fun runInitBootstrap(
    command: InitCommand,
    ideRefreshRunner: (IdeRefreshCommand) -> IdeHelperRefreshResult = ::refreshIdeHelperProject,
): InitBootstrapResult {
    val projectRoot = command.projectRoot.toAbsolutePath().normalize()
    require(Files.exists(projectRoot)) {
        "Repository root '$projectRoot' does not exist."
    }
    require(Files.isDirectory(projectRoot)) {
        "Repository root '$projectRoot' is not a directory."
    }

    val createdFiles = mutableListOf<Path>()
    val preservedFiles = mutableListOf<Path>()
    DEFAULT_BOOTSTRAP_FILES.forEach { (relativePath, content) ->
        val path = projectRoot.resolve(relativePath)
        when {
            Files.exists(path) && Files.isRegularFile(path) -> preservedFiles.add(path)
            Files.exists(path) ->
                throw InitConflictException("Bootstrap path '$path' exists but is not a regular file.")

            else -> {
                Files.createDirectories(path.parent)
                Files.writeString(path, content, StandardCharsets.UTF_8)
                createdFiles.add(path)
            }
        }
    }

    val ideHelperResult =
        ideRefreshRunner(
            IdeRefreshCommand(
                projectRoot = projectRoot,
                diagnosticsFormat = command.diagnosticsFormat,
                verbose = command.verbose,
            ),
        )

    return InitBootstrapResult(
        projectRoot = projectRoot,
        createdFiles = createdFiles.sortedBy(Path::toString),
        preservedFiles = preservedFiles.sortedBy(Path::toString),
        ideHelperResult = ideHelperResult,
    )
}

private val DEFAULT_BOOTSTRAP_FILES =
    linkedMapOf(
        "settings.microsmith.kts" to renderDefaultSettingsScript(),
        "build.microsmith.kts" to renderDefaultBuildScript(),
    )

private fun renderDefaultSettingsScript(): String = """
// Microsmith repository settings.
// Add shared script configuration here as your repository grows.
""".trimIndent() + "\n"

private fun renderDefaultBuildScript(): String = """
microsmith {
    schemas {
        protobuf {
            message("UserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
""".trimIndent() + "\n"
