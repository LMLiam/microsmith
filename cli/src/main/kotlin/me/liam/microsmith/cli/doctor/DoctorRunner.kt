package me.liam.microsmith.cli.doctor

import me.liam.microsmith.cli.plugins.defaultPluginCacheDirectory
import me.liam.microsmith.cli.plugins.defaultRepositoryAllowlistPolicy
import java.nio.file.Files
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
): DoctorResult {
    val checks =
        listOf(
            checkJavaRuntime(),
            checkProviderDiscovery(providerValidator),
            checkDirectoryWritable(id = "script-cache", directory = scriptCacheDirectory),
            checkDirectoryWritable(id = "plugin-cache", directory = pluginCacheDirectory),
            checkRepositoryPolicy(),
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

private fun defaultScriptCacheDirectory(): Path {
    val envPath = System.getenv("MICROSMITH_SCRIPT_CACHE_DIR")?.trim()?.takeIf { it.isNotEmpty() }
    return if (envPath != null) {
        Path.of(envPath)
    } else {
        Path.of(System.getProperty("user.home"), ".microsmith", "cache", "scripts")
    }
}

private const val MIN_SUPPORTED_JAVA_FEATURE = 24
