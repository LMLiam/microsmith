package me.liam.microsmith.cli.ide

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

internal object IdeHelperDoctorChecks {
    fun helperDirectoryCheck(helperRoot: Path): IdeDoctorCheckResult = when {
        managedPathExists(helperRoot) && Files.isDirectory(helperRoot, LinkOption.NOFOLLOW_LINKS) ->
            IdeDoctorCheckResult(
                id = "helper-directory",
                passed = true,
                message = "IDE helper directory exists.",
                details = mapOf("helperRoot" to helperRoot.toString()),
            )

        managedPathExists(helperRoot) ->
            IdeDoctorCheckResult(
                id = "helper-directory",
                passed = false,
                message = "IDE helper path is invalid. Remove the conflicting path and run 'microsmith ide refresh'.",
                details = mapOf("helperRoot" to helperRoot.toString()),
            )

        else ->
            IdeDoctorCheckResult(
                id = "helper-directory",
                passed = false,
                message = "IDE helper directory is missing. Run 'microsmith ide refresh'.",
                details = mapOf("helperRoot" to helperRoot.toString()),
            )
    }

    fun requiredFilesCheck(helperRoot: Path): IdeDoctorCheckResult {
        val requiredFiles = IdeHelperManagedSurface.requiredFiles(helperRoot)
        val invalidFiles =
            requiredFiles
                .filter(::managedPathExists)
                .filterNot(::isManagedRegularFile)
                .sortedBy(Path::toString)
        val missingFiles = requiredFiles.filterNot(::managedPathExists).sortedBy(Path::toString)
        return when {
            invalidFiles.isNotEmpty() ->
                IdeDoctorCheckResult(
                    id = "required-files",
                    passed = false,
                    message =
                    "IDE helper contains conflicting managed paths. Remove them and run 'microsmith ide refresh'.",
                    details =
                    mapOf(
                        "invalidCount" to invalidFiles.size.toString(),
                        "invalidFiles" to invalidFiles.joinToString(separator = ","),
                    ),
                )

            missingFiles.isEmpty() ->
                IdeDoctorCheckResult(
                    id = "required-files",
                    passed = true,
                    message = "All required IDE helper files are present.",
                    details = mapOf("fileCount" to requiredFiles.size.toString()),
                )

            else ->
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

    fun runtimeClasspathCheck(classpathEntries: List<Path>): IdeDoctorCheckResult = if (classpathEntries.isEmpty()) {
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

    fun classpathSyncCheck(helperRoot: Path, classpathEntries: List<Path>): IdeDoctorCheckResult {
        val buildFile = IdeHelperManagedSurface.buildFile(helperRoot)
        val buildFileContent = readManagedUtf8TextOrNull(buildFile)
        val missingClasspathEntries =
            buildFileContent
                ?.let { content ->
                    classpathEntries.filterNot { entry -> content.contains(entry.toKotlinPathLiteral()) }
                }
                ?: classpathEntries
        if (missingClasspathEntries.isEmpty()) {
            return IdeDoctorCheckResult(
                id = "classpath-sync",
                passed = true,
                message = "IDE helper build file is synchronized with runtime classpath.",
            )
        }
        return IdeDoctorCheckResult(
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

    private fun managedPathExists(path: Path): Boolean = Files.exists(path, LinkOption.NOFOLLOW_LINKS)

    private fun isManagedRegularFile(path: Path): Boolean = Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)

    private fun readManagedUtf8TextOrNull(path: Path): String? {
        return try {
            if (!isManagedRegularFile(path)) {
                return null
            }
            Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n")
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
}
