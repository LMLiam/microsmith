package io.github.lmliam.microsmith.cli.doctor

import io.github.lmliam.microsmith.cli.ide.IDE_HELPER_DIRECTORY
import io.github.lmliam.microsmith.cli.ide.IdeHelperManagedSurface
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

internal object DoctorBootstrapStateCheck {
    fun check(projectRoot: Path): DoctorCheckResult {
        val buildScript = projectRoot.resolve(INIT_BUILD_FILE_NAME)
        val settingsScript = projectRoot.resolve(INIT_SETTINGS_FILE_NAME)
        val helperRoot = projectRoot.resolve(IDE_HELPER_DIRECTORY)

        validateBootstrapSurface(
            projectRoot = projectRoot,
            buildScript = buildScript,
            settingsScript = settingsScript,
            helperRoot = helperRoot,
        )?.let { return it }

        val invalidHelperFiles = invalidIdeHelperFiles(projectRoot = projectRoot, helperRoot = helperRoot)
        val missingHelperFiles = missingIdeHelperFiles(projectRoot = projectRoot, helperRoot = helperRoot)

        return when {
            invalidHelperFiles.isNotEmpty() ->
                failure(
                    message =
                    "JetBrains IDE helper contains conflicting managed paths. " +
                        "Remove them and run 'microsmith ide refresh' to repair it.",
                    details = mapOf("invalidIdeHelperFiles" to invalidHelperFiles.joinToString(separator = ",")),
                )

            missingHelperFiles.isNotEmpty() ->
                failure(
                    message = "JetBrains IDE helper is incomplete. Run 'microsmith ide refresh' to repair it.",
                    details = mapOf("missingIdeHelperFiles" to missingHelperFiles.joinToString(separator = ",")),
                )

            Files.isDirectory(helperRoot, LinkOption.NOFOLLOW_LINKS) ->
                pass(
                    message = "Bootstrap files and JetBrains IDE helper are present.",
                    details = mapOf("projectRoot" to projectRoot.toString()),
                )

            else ->
                failure(
                    message =
                    "Bootstrap files are present, but the JetBrains IDE helper is missing. " +
                        "Run 'microsmith ide refresh' to restore the default onboarding surface.",
                    details = mapOf("projectRoot" to projectRoot.toString()),
                )
        }
    }

    private fun validateBootstrapSurface(
        projectRoot: Path,
        buildScript: Path,
        settingsScript: Path,
        helperRoot: Path,
    ): DoctorCheckResult? {
        val hasManagedSurface =
            Files.exists(buildScript, LinkOption.NOFOLLOW_LINKS) ||
                Files.exists(settingsScript, LinkOption.NOFOLLOW_LINKS) ||
                Files.exists(helperRoot, LinkOption.NOFOLLOW_LINKS)
        if (!hasManagedSurface) {
            return pass(
                message = "Bootstrap files were not detected in the current working directory.",
                details = mapOf("projectRoot" to projectRoot.toString()),
            )
        }

        val bootstrapFiles = listOf(buildScript, settingsScript)
        val invalidBootstrapFiles = invalidManagedFiles(projectRoot = projectRoot, managedFiles = bootstrapFiles)
        val missingBootstrapFiles = missingManagedFiles(projectRoot = projectRoot, managedFiles = bootstrapFiles)

        return when {
            invalidBootstrapFiles.isNotEmpty() ->
                failure(
                    message =
                    "Bootstrap paths are invalid. Remove the conflicting paths. " +
                        "Run 'microsmith init' to repair them.",
                    details =
                    mapOf(
                        "invalidBootstrapFiles" to invalidBootstrapFiles.joinToString(separator = ","),
                    ),
                )

            missingBootstrapFiles.isNotEmpty() ->
                failure(
                    message = "Bootstrap state is incomplete. Run 'microsmith init' to repair it.",
                    details =
                    mapOf(
                        "missingBootstrapFiles" to missingBootstrapFiles.joinToString(separator = ","),
                    ),
                )

            managedPathExists(helperRoot) && !Files.isDirectory(helperRoot, LinkOption.NOFOLLOW_LINKS) ->
                failure(
                    message =
                    "JetBrains IDE helper path is invalid. " +
                        "Run 'microsmith ide refresh' after removing the conflicting path.",
                    details = mapOf("helperRoot" to helperRoot.toString()),
                )

            else -> null
        }
    }

    private fun invalidIdeHelperFiles(projectRoot: Path, helperRoot: Path): List<String> =
        IdeHelperManagedSurface.requiredFiles(helperRoot)
            .takeIf { Files.isDirectory(helperRoot, LinkOption.NOFOLLOW_LINKS) }
            ?.let { managedFiles ->
                invalidManagedFiles(projectRoot = projectRoot, managedFiles = managedFiles)
            }
            .orEmpty()

    private fun missingIdeHelperFiles(projectRoot: Path, helperRoot: Path): List<String> =
        IdeHelperManagedSurface.requiredFiles(helperRoot)
            .takeIf { Files.isDirectory(helperRoot, LinkOption.NOFOLLOW_LINKS) }
            ?.let { managedFiles ->
                missingManagedFiles(projectRoot = projectRoot, managedFiles = managedFiles)
            }
            .orEmpty()

    private fun missingManagedFiles(projectRoot: Path, managedFiles: List<Path>): List<String> = managedFiles
        .filterNot(::managedPathExists)
        .map(projectRoot::relativize)
        .map(Path::toString)
        .sorted()

    private fun invalidManagedFiles(projectRoot: Path, managedFiles: List<Path>): List<String> = managedFiles
        .filter(::managedPathExists)
        .filterNot(::isManagedRegularFile)
        .map(projectRoot::relativize)
        .map(Path::toString)
        .sorted()

    private fun managedPathExists(path: Path): Boolean = Files.exists(path, LinkOption.NOFOLLOW_LINKS)

    private fun isManagedRegularFile(path: Path): Boolean = Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)

    private fun pass(message: String, details: Map<String, String> = emptyMap()): DoctorCheckResult = DoctorCheckResult(
        id = CHECK_ID,
        status = DoctorCheckStatus.PASS,
        message = message,
        details = details,
    )

    private fun failure(message: String, details: Map<String, String> = emptyMap()): DoctorCheckResult =
        DoctorCheckResult(
            id = CHECK_ID,
            status = DoctorCheckStatus.FAIL,
            message = message,
            details = details,
        )
}

private const val CHECK_ID = "bootstrap-state"
private const val INIT_BUILD_FILE_NAME = "build.microsmith.kts"
private const val INIT_SETTINGS_FILE_NAME = "settings.microsmith.kts"
