package io.github.lmliam.microsmith.runtime.scripting.host.paths

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.plugin.Environment
import java.nio.file.FileSystem
import java.nio.file.Path

@Inject
internal class ScriptCacheDirectoryPolicy(
    private val environment: Environment,
    private val fileSystem: FileSystem,
    private val configuredDirectory: Path?,
) {
    fun resolve(): Path =
        configuredDirectory
            ?: environment
                .variable(CACHE_DIRECTORY_VARIABLE)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(fileSystem::getPath)
            ?: fileSystem.getPath(
                checkNotNull(environment.property(USER_HOME_PROPERTY)) { "$USER_HOME_PROPERTY is not set" },
                ".microsmith",
                "cache",
                "scripts",
            )

    internal companion object {
        const val CACHE_DIRECTORY_VARIABLE = "MICROSMITH_SCRIPT_CACHE_DIR"
        const val USER_HOME_PROPERTY = "user.home"
    }
}
