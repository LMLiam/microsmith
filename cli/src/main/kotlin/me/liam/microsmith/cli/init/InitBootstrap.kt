package me.liam.microsmith.cli.init

import me.liam.microsmith.cli.command.IdeRefreshCommand
import me.liam.microsmith.cli.command.InitCommand
import me.liam.microsmith.cli.ide.IdeHelperConflictException
import me.liam.microsmith.cli.ide.IdeHelperRefreshResult
import me.liam.microsmith.cli.ide.refreshIdeHelperProject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

internal data class InitBootstrapResult(
    val projectRoot: Path,
    val repositoryDetection: OnboardingRepositoryDetection,
    val createdFiles: List<Path>,
    val overwrittenFiles: List<Path>,
    val preservedFiles: List<Path>,
    val ideHelperResult: IdeHelperRefreshResult?,
)

internal class InitConflictException(message: String) : IllegalStateException(message)
internal class InitValidationException(message: String) : IllegalArgumentException(message)

internal fun runInitBootstrap(
    command: InitCommand,
    ideRefreshRunner: (IdeRefreshCommand) -> IdeHelperRefreshResult = ::refreshIdeHelperProject,
): InitBootstrapResult {
    val projectRoot = command.projectRoot.toAbsolutePath().normalize()
    requireInit(Files.exists(projectRoot)) {
        "Repository root '$projectRoot' does not exist."
    }
    requireInit(Files.isDirectory(projectRoot)) {
        "Repository root '$projectRoot' is not a directory."
    }

    val repositoryDetection = detectOnboardingRepositoryType(projectRoot)
    val createdFiles = mutableListOf<Path>()
    val overwrittenFiles = mutableListOf<Path>()
    val preservedFiles = mutableListOf<Path>()

    bootstrapFilesFor(repositoryDetection).forEach { (relativePath, content) ->
        val path = projectRoot.resolve(relativePath)
        when {
            Files.exists(path, LinkOption.NOFOLLOW_LINKS) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) -> {
                val existingContent = Files.readString(path, StandardCharsets.UTF_8)
                if (command.force && existingContent.normalizeLineEndings() != content.normalizeLineEndings()) {
                    Files.writeString(path, content, StandardCharsets.UTF_8)
                    overwrittenFiles.add(path)
                } else {
                    preservedFiles.add(path)
                }
            }

            Files.exists(path, LinkOption.NOFOLLOW_LINKS) ->
                throw InitConflictException("Bootstrap path '$path' exists but is not a regular file.")

            else -> {
                Files.createDirectories(path.parent)
                Files.writeString(path, content, StandardCharsets.UTF_8)
                createdFiles.add(path)
            }
        }
    }

    val ideHelperResult =
        if (command.skipIdeHelper) {
            null
        } else {
            runCatching {
                ideRefreshRunner(
                    IdeRefreshCommand(
                        projectRoot = projectRoot,
                        diagnosticsFormat = command.diagnosticsFormat,
                        verbose = command.verbose,
                    ),
                )
            }.getOrElse { error ->
                when (error) {
                    is IdeHelperConflictException -> throw InitConflictException(error.message ?: "IDE helper path is invalid.")
                    else -> throw error
                }
            }
        }

    return InitBootstrapResult(
        projectRoot = projectRoot,
        repositoryDetection = repositoryDetection,
        createdFiles = createdFiles.sortedBy(Path::toString),
        overwrittenFiles = overwrittenFiles.sortedBy(Path::toString),
        preservedFiles = preservedFiles.sortedBy(Path::toString),
        ideHelperResult = ideHelperResult,
    )
}

private fun bootstrapFilesFor(repositoryDetection: OnboardingRepositoryDetection): Map<String, String> = linkedMapOf(
    "settings.microsmith.kts" to renderDefaultSettingsScript(repositoryDetection),
    "build.microsmith.kts" to renderDefaultBuildScript(repositoryDetection.type),
)

private fun renderDefaultSettingsScript(repositoryDetection: OnboardingRepositoryDetection): String = buildString {
    appendLine("// Microsmith repository settings.")
    appendLine("// ${repositoryDetection.describeForComment()}.")
    appendLine("// Add shared script configuration here as your repository grows.")
}

private fun renderDefaultBuildScript(repositoryType: OnboardingRepositoryType): String = buildString {
    val displayName =
        if (repositoryType == OnboardingRepositoryType.OTHER) {
            "repository"
        } else {
            "${repositoryType.displayName} repository"
        }
    appendLine("// Bootstrapped Microsmith schema for this $displayName.")
    appendLine("// Canonical first run:")
    appendLine("// microsmith run build.microsmith.kts --out ./generated")
    repositoryType.repoNativeOutputDirectory?.let { outputDirectory ->
        appendLine("// Common repository-native output path:")
        appendLine("// microsmith run build.microsmith.kts --out $outputDirectory")
    }
    appendLine(
        """
        microsmith {
            schemas {
                protobuf {
                    message("${repositoryType.sampleMessageName}") {
                        int32("id") { index(1) }
                        string("email") { index(2) }
                    }
                }
            }
        }
        """.trimIndent(),
    )
}

private fun String.normalizeLineEndings(): String = replace("\r\n", "\n")

private inline fun requireInit(value: Boolean, lazyMessage: () -> String) {
    if (!value) {
        throw InitValidationException(lazyMessage())
    }
}
