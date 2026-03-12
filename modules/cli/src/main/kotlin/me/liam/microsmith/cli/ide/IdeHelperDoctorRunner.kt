package me.liam.microsmith.cli.ide

import me.liam.microsmith.cli.command.IdeDoctorCommand
import java.nio.file.Files
import java.nio.file.Path

internal class IdeHelperDoctorRunner(
    private val command: IdeDoctorCommand,
    private val classpathResolver: () -> List<Path> = ::resolveIdeHelperClasspathEntries,
) {
    fun run(): IdeDoctorResult {
        val projectRoot = command.projectRoot.toAbsolutePath().normalize()
        val helperRoot = projectRoot.resolve(IDE_HELPER_DIRECTORY).toAbsolutePath().normalize()
        validateRepoRoot(projectRoot, helperRoot)?.let { return it }

        val classpathEntries = resolveClasspathEntries()
        val runtimeClasspathCheck = IdeHelperDoctorChecks.runtimeClasspathCheck(classpathEntries)
        val checks =
            listOf(
                IdeHelperDoctorChecks.helperDirectoryCheck(helperRoot),
                IdeHelperDoctorChecks.requiredFilesCheck(helperRoot),
                runtimeClasspathCheck,
                IdeHelperDoctorChecks.classpathSyncCheck(helperRoot, classpathEntries),
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

    private fun resolveClasspathEntries(): List<Path> = classpathResolver()
        .map { path -> path.toAbsolutePath().normalize() }
        .filter(Files::exists)
        .distinctBy(Path::toString)
        .sortedBy(Path::toString)
}

internal fun runIdeHelperDoctor(
    command: IdeDoctorCommand,
    classpathResolver: () -> List<Path> = ::resolveIdeHelperClasspathEntries,
): IdeDoctorResult = IdeHelperDoctorRunner(command = command, classpathResolver = classpathResolver).run()
