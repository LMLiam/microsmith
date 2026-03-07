package me.liam.microsmith.cli.ide

import me.liam.microsmith.cli.command.IdeRefreshCommand
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

internal class IdeHelperProjectGenerator(
    private val command: IdeRefreshCommand,
    private val classpathResolver: () -> List<Path> = ::resolveIdeHelperClasspathEntries,
    private val fileWriter: ManagedIdeHelperFileWriter = ManagedIdeHelperFileWriter(),
) {
    fun run(): IdeHelperRefreshResult {
        val projectRoot = command.projectRoot.toAbsolutePath().normalize()
        require(Files.exists(projectRoot)) {
            "Repository root '$projectRoot' does not exist."
        }
        require(Files.isDirectory(projectRoot)) {
            "Repository root '$projectRoot' is not a directory."
        }

        val helperRoot = projectRoot.resolve(IDE_HELPER_DIRECTORY).toAbsolutePath().normalize()
        val classpathEntries =
            classpathResolver()
                .map { path -> path.toAbsolutePath().normalize() }
                .filter(Files::exists)
                .distinctBy(Path::toString)
                .sortedBy(Path::toString)

        require(classpathEntries.isNotEmpty()) {
            "Could not resolve runtime classpath entries for IDE helper generation."
        }

        fileWriter.ensureManagedDirectory(projectRoot.resolve(".microsmith").toAbsolutePath().normalize())
        fileWriter.ensureManagedDirectory(helperRoot)

        val updatedFiles =
            buildList {
                IdeHelperManagedSurface.renderedFiles(helperRoot, classpathEntries).forEach { (path, content) ->
                    if (fileWriter.writeFileIfChanged(path, content)) {
                        add(path)
                    }
                }
            }.sortedBy(Path::toString)

        return IdeHelperRefreshResult(
            projectRoot = projectRoot,
            helperRoot = helperRoot,
            updatedFiles = updatedFiles,
            classpathEntries = classpathEntries,
        )
    }
}

internal fun refreshIdeHelperProject(
    command: IdeRefreshCommand,
    classpathResolver: () -> List<Path> = ::resolveIdeHelperClasspathEntries,
): IdeHelperRefreshResult = IdeHelperProjectGenerator(command = command, classpathResolver = classpathResolver).run()

internal fun resolveIdeHelperClasspathEntries(
    javaClasspath: String = System.getProperty("java.class.path").orEmpty(),
): List<Path> {
    if (javaClasspath.isBlank()) {
        return emptyList()
    }

    return javaClasspath
        .split(File.pathSeparator)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map(Path::of)
}
