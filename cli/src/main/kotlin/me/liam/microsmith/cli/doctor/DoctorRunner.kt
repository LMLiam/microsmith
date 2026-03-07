package me.liam.microsmith.cli.doctor

import me.liam.microsmith.cli.ide.IDE_HELPER_BUILD_FILE_NAME
import me.liam.microsmith.cli.ide.IDE_HELPER_DIRECTORY
import me.liam.microsmith.cli.ide.IDE_HELPER_README_FILE_NAME
import me.liam.microsmith.cli.ide.IDE_HELPER_SETTINGS_FILE_NAME
import me.liam.microsmith.cli.plugins.defaultPluginCacheDirectory
import me.liam.microsmith.cli.plugins.defaultRepositoryAllowlistPolicy
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.ServiceConfigurationError
import kotlin.io.path.deleteIfExists

internal enum class DoctorCheckStatus {
    PASS,
    FAIL,
}

internal data class DoctorCheckResult(
    val id: String,
    val status: DoctorCheckStatus,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)

internal data class DoctorResult(val checks: List<DoctorCheckResult>) {
    val hasFailures: Boolean
        get() = checks.any { it.status == DoctorCheckStatus.FAIL }
}

internal fun runDoctorChecks(
    providerValidator: () -> List<String>,
    scriptCacheDirectory: Path = defaultScriptCacheDirectory(),
    pluginCacheDirectory: Path = defaultPluginCacheDirectory(),
    projectRoot: Path = Path.of(".").toAbsolutePath().normalize(),
): DoctorResult {
    val checks =
        listOf(
            checkJavaRuntime(),
            checkProviderDiscovery(providerValidator),
            checkDirectoryWritable(id = "script-cache", directory = scriptCacheDirectory),
            checkDirectoryWritable(id = "plugin-cache", directory = pluginCacheDirectory),
            checkRepositoryPolicy(),
            checkBootstrapState(projectRoot),
        )
    return DoctorResult(checks)
}

private fun checkJavaRuntime(): DoctorCheckResult {
    val feature = Runtime.version().feature()
    return if (feature >= MIN_SUPPORTED_JAVA_FEATURE) {
        DoctorCheckResult(
            id = "java-runtime",
            status = DoctorCheckStatus.PASS,
            message = "Detected Java runtime feature $feature (minimum is $MIN_SUPPORTED_JAVA_FEATURE).",
            details = mapOf("feature" to feature.toString()),
        )
    } else {
        DoctorCheckResult(
            id = "java-runtime",
            status = DoctorCheckStatus.FAIL,
            message =
            "Detected Java runtime feature $feature, but Microsmith requires at least " +
                "$MIN_SUPPORTED_JAVA_FEATURE.",
            details = mapOf("feature" to feature.toString()),
        )
    }
}

private fun checkProviderDiscovery(providerValidator: () -> List<String>): DoctorCheckResult = try {
    val errors = providerValidator()
    if (errors.isEmpty()) {
        DoctorCheckResult(
            id = "provider-discovery",
            status = DoctorCheckStatus.PASS,
            message = "Required built-in service providers are available.",
        )
    } else {
        DoctorCheckResult(
            id = "provider-discovery",
            status = DoctorCheckStatus.FAIL,
            message = "Required built-in service providers are missing.",
            details = mapOf("errors" to errors.joinToString(" | ")),
        )
    }
} catch (error: ServiceConfigurationError) {
    DoctorCheckResult(
        id = "provider-discovery",
        status = DoctorCheckStatus.FAIL,
        message = "Service provider loading failed.",
        details = mapOf("error" to (error.message ?: error::class.simpleName.orEmpty())),
    )
}

private fun checkDirectoryWritable(id: String, directory: Path): DoctorCheckResult = runCatching {
    Files.createDirectories(directory)
    val probe = Files.createTempFile(directory, "microsmith-doctor-", ".tmp")
    probe.deleteIfExists()
    DoctorCheckResult(
        id = id,
        status = DoctorCheckStatus.PASS,
        message = "Directory is writable.",
        details = mapOf("path" to directory.toAbsolutePath().normalize().toString()),
    )
}.getOrElse { error ->
    DoctorCheckResult(
        id = id,
        status = DoctorCheckStatus.FAIL,
        message = "Directory is not writable.",
        details =
        mapOf(
            "path" to directory.toAbsolutePath().normalize().toString(),
            "error" to (error.message ?: error::class.simpleName.orEmpty()),
        ),
    )
}

private fun checkRepositoryPolicy(): DoctorCheckResult = runCatching {
    val policy = defaultRepositoryAllowlistPolicy()
    DoctorCheckResult(
        id = "repository-policy",
        status = DoctorCheckStatus.PASS,
        message = "Repository allowlist policy initialized successfully.",
        details = mapOf("allowFileRepositories" to policy.allowFileRepositories.toString()),
    )
}.getOrElse { error ->
    DoctorCheckResult(
        id = "repository-policy",
        status = DoctorCheckStatus.FAIL,
        message = "Repository allowlist policy could not be initialized.",
        details = mapOf("error" to (error.message ?: error::class.simpleName.orEmpty())),
    )
}

private fun checkBootstrapState(projectRoot: Path): DoctorCheckResult {
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
            DoctorCheckResult(
                id = "bootstrap-state",
                status = DoctorCheckStatus.FAIL,
                message =
                "JetBrains IDE helper contains conflicting managed paths. " +
                    "Remove them and run 'microsmith ide refresh' to repair it.",
                details =
                mapOf(
                    "invalidIdeHelperFiles" to invalidHelperFiles.joinToString(separator = ","),
                ),
            )

        missingHelperFiles.isNotEmpty() ->
            DoctorCheckResult(
                id = "bootstrap-state",
                status = DoctorCheckStatus.FAIL,
                message = "JetBrains IDE helper is incomplete. Run 'microsmith ide refresh' to repair it.",
                details =
                mapOf(
                    "missingIdeHelperFiles" to missingHelperFiles.joinToString(separator = ","),
                ),
            )

        Files.isDirectory(helperRoot, LinkOption.NOFOLLOW_LINKS) ->
            DoctorCheckResult(
                id = "bootstrap-state",
                status = DoctorCheckStatus.PASS,
                message = "Bootstrap files and JetBrains IDE helper are present.",
                details = mapOf("projectRoot" to projectRoot.toString()),
            )

        else ->
            DoctorCheckResult(
                id = "bootstrap-state",
                status = DoctorCheckStatus.FAIL,
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
        return DoctorCheckResult(
            id = "bootstrap-state",
            status = DoctorCheckStatus.PASS,
            message = "Bootstrap files were not detected in the current working directory.",
            details = mapOf("projectRoot" to projectRoot.toString()),
        )
    }

    val bootstrapFiles = listOf(buildScript, settingsScript)
    val invalidBootstrapFiles = invalidManagedFiles(projectRoot = projectRoot, managedFiles = bootstrapFiles)
    if (invalidBootstrapFiles.isNotEmpty()) {
        return DoctorCheckResult(
            id = "bootstrap-state",
            status = DoctorCheckStatus.FAIL,
            message =
            "Bootstrap paths are invalid. Remove the conflicting paths and run 'microsmith init' to repair them.",
            details =
            mapOf(
                "invalidBootstrapFiles" to invalidBootstrapFiles.joinToString(separator = ","),
            ),
        )
    }

    val missingBootstrapFiles = missingManagedFiles(projectRoot = projectRoot, managedFiles = bootstrapFiles)
    if (missingBootstrapFiles.isNotEmpty()) {
        return DoctorCheckResult(
            id = "bootstrap-state",
            status = DoctorCheckStatus.FAIL,
            message = "Bootstrap state is incomplete. Run 'microsmith init' to repair it.",
            details =
            mapOf(
                "missingBootstrapFiles" to missingBootstrapFiles.joinToString(separator = ","),
            ),
        )
    }

    return if (
        Files.exists(helperRoot, LinkOption.NOFOLLOW_LINKS) &&
        !Files.isDirectory(helperRoot, LinkOption.NOFOLLOW_LINKS)
    ) {
        DoctorCheckResult(
            id = "bootstrap-state",
            status = DoctorCheckStatus.FAIL,
            message =
            "JetBrains IDE helper path is invalid. " +
                "Run 'microsmith ide refresh' after removing the conflicting path.",
            details = mapOf("helperRoot" to helperRoot.toString()),
        )
    } else {
        null
    }
}

private fun missingIdeHelperFiles(projectRoot: Path, helperRoot: Path): List<String> =
    requiredIdeHelperFiles(helperRoot)
        .takeIf { Files.isDirectory(helperRoot, LinkOption.NOFOLLOW_LINKS) }
        ?.let { managedFiles ->
            missingManagedFiles(projectRoot = projectRoot, managedFiles = managedFiles)
        } ?: emptyList()

private fun invalidIdeHelperFiles(projectRoot: Path, helperRoot: Path): List<String> =
    requiredIdeHelperFiles(helperRoot)
        .takeIf { Files.isDirectory(helperRoot, LinkOption.NOFOLLOW_LINKS) }
        ?.let { managedFiles ->
            invalidManagedFiles(projectRoot = projectRoot, managedFiles = managedFiles)
        } ?: emptyList()

private fun requiredIdeHelperFiles(helperRoot: Path): List<Path> = listOf(
    helperRoot.resolve(IDE_HELPER_SETTINGS_FILE_NAME),
    helperRoot.resolve(IDE_HELPER_BUILD_FILE_NAME),
    helperRoot.resolve(IDE_HELPER_README_FILE_NAME),
)

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

private fun defaultScriptCacheDirectory(): Path {
    val envPath = System.getenv("MICROSMITH_SCRIPT_CACHE_DIR")?.trim()?.takeIf { it.isNotEmpty() }
    return if (envPath != null) {
        Path.of(envPath)
    } else {
        Path.of(System.getProperty("user.home"), ".microsmith", "cache", "scripts")
    }
}

private const val INIT_BUILD_FILE_NAME = "build.microsmith.kts"
private const val INIT_SETTINGS_FILE_NAME = "settings.microsmith.kts"
private const val MIN_SUPPORTED_JAVA_FEATURE = 24
