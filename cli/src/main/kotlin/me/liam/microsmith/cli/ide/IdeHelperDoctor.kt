package me.liam.microsmith.cli.ide

import me.liam.microsmith.cli.command.IdeDoctorCommand
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

internal data class IdeDoctorCheckResult(
    val id: String,
    val passed: Boolean,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)

internal data class IdeDoctorResult(
    val projectRoot: Path,
    val helperRoot: Path,
    val checks: List<IdeDoctorCheckResult>,
) {
    val hasFailures: Boolean = checks.any { check -> !check.passed }
}

internal fun runIdeHelperDoctor(
    command: IdeDoctorCommand,
    classpathResolver: () -> List<Path> = ::resolveIdeHelperClasspathEntries,
): IdeDoctorResult {
    val projectRoot = command.projectRoot.toAbsolutePath().normalize()
    val helperRoot = projectRoot.resolve(IDE_HELPER_DIRECTORY).toAbsolutePath().normalize()
    validateRepoRoot(projectRoot, helperRoot)?.let { return it }

    val classpathEntries = resolveClasspathEntries(classpathResolver)
    val runtimeClasspathCheck = runtimeClasspathCheck(classpathEntries)
    val checks =
        listOf(
            helperDirectoryCheck(helperRoot),
            requiredFilesCheck(helperRoot),
            runtimeClasspathCheck,
            classpathSyncCheck(helperRoot, classpathEntries),
        )

    return IdeDoctorResult(projectRoot = projectRoot, helperRoot = helperRoot, checks = checks)
}

private fun validateRepoRoot(projectRoot: Path, helperRoot: Path): IdeDoctorResult? {
    if (!Files.exists(projectRoot)) {
        return IdeDoctorResult(
            projectRoot = projectRoot,
            helperRoot = helperRoot,
            checks =
            listOf(
                IdeDoctorCheckResult(
                    id = "repo-root",
                    passed = false,
                    message = "Repository root '$projectRoot' does not exist.",
                ),
            ),
        )
    }

    if (!Files.isDirectory(projectRoot)) {
        return IdeDoctorResult(
            projectRoot = projectRoot,
            helperRoot = helperRoot,
            checks =
            listOf(
                IdeDoctorCheckResult(
                    id = "repo-root",
                    passed = false,
                    message = "Repository root '$projectRoot' is not a directory.",
                ),
            ),
        )
    }

    return null
}

private fun helperDirectoryCheck(helperRoot: Path): IdeDoctorCheckResult = if (Files.isDirectory(helperRoot)) {
    IdeDoctorCheckResult(
        id = "helper-directory",
        passed = true,
        message = "IDE helper directory exists.",
        details = mapOf("helperRoot" to helperRoot.toString()),
    )
} else {
    IdeDoctorCheckResult(
        id = "helper-directory",
        passed = false,
        message = "IDE helper directory is missing. Run 'microsmith ide refresh'.",
        details = mapOf("helperRoot" to helperRoot.toString()),
    )
}

private fun requiredFilesCheck(helperRoot: Path): IdeDoctorCheckResult {
    val requiredFiles =
        listOf(
            helperRoot.resolve(IDE_HELPER_SETTINGS_FILE_NAME),
            helperRoot.resolve(IDE_HELPER_BUILD_FILE_NAME),
            helperRoot.resolve(IDE_HELPER_README_FILE_NAME),
        )
    val missingFiles = requiredFiles.filterNot(Files::isRegularFile).sortedBy(Path::toString)
    return if (missingFiles.isEmpty()) {
        IdeDoctorCheckResult(
            id = "required-files",
            passed = true,
            message = "All required IDE helper files are present.",
            details = mapOf("fileCount" to requiredFiles.size.toString()),
        )
    } else {
        IdeDoctorCheckResult(
            id = "required-files",
            passed = false,
            message = "IDE helper files are missing. Run 'microsmith ide refresh'.",
            details =
            mapOf(
                "missingCount" to missingFiles.size.toString(),
                "missingFiles" to missingFiles.joinToString(separator = ","),
            ),
        )
    }
}

private fun resolveClasspathEntries(classpathResolver: () -> List<Path>): List<Path> = classpathResolver()
    .map { path -> path.toAbsolutePath().normalize() }
    .filter(Files::exists)
    .distinctBy(Path::toString)
    .sortedBy(Path::toString)

private fun runtimeClasspathCheck(classpathEntries: List<Path>): IdeDoctorCheckResult =
    if (classpathEntries.isEmpty()) {
        IdeDoctorCheckResult(
            id = "runtime-classpath",
            passed = false,
            message = "Could not resolve runtime classpath entries for IDE helper validation.",
        )
    } else {
        IdeDoctorCheckResult(
            id = "runtime-classpath",
            passed = true,
            message = "Resolved runtime classpath entries.",
            details = mapOf("classpathEntries" to classpathEntries.size.toString()),
        )
    }

private fun classpathSyncCheck(helperRoot: Path, classpathEntries: List<Path>): IdeDoctorCheckResult {
    val buildFile = helperRoot.resolve(IDE_HELPER_BUILD_FILE_NAME)
    val buildFileContent =
        if (Files.isRegularFile(buildFile)) {
            Files.readString(buildFile, StandardCharsets.UTF_8).replace("\r\n", "\n")
        } else {
            null
        }
    val missingClasspathEntries =
        if (buildFileContent == null) {
            classpathEntries
        } else {
            classpathEntries.filterNot { entry -> buildFileContent.contains(entry.toKotlinPathLiteral()) }
        }
    return if (missingClasspathEntries.isEmpty()) {
        IdeDoctorCheckResult(
            id = "classpath-sync",
            passed = true,
            message = "IDE helper build file is synchronized with runtime classpath.",
        )
    } else {
        IdeDoctorCheckResult(
            id = "classpath-sync",
            passed = false,
            message = "IDE helper build file is stale. Run 'microsmith ide refresh'.",
            details =
            mapOf(
                "missingClasspathEntries" to missingClasspathEntries.size.toString(),
                "firstMissingClasspathEntry" to missingClasspathEntries.first().toString(),
            ),
        )
    }
}
