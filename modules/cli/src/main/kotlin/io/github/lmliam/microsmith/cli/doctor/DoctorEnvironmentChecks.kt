package io.github.lmliam.microsmith.cli.doctor

import io.github.lmliam.microsmith.cli.plugins.defaultRepositoryAllowlistPolicy
import java.nio.file.Files
import java.nio.file.Path
import java.util.ServiceConfigurationError
import kotlin.io.path.deleteIfExists

internal object DoctorEnvironmentChecks {
    fun checkJavaRuntime(minSupportedJavaFeature: Int = MIN_SUPPORTED_JAVA_FEATURE): DoctorCheckResult {
        val feature = Runtime.version().feature()
        if (feature < minSupportedJavaFeature) {
            return DoctorCheckResult(
                id = "java-runtime",
                status = DoctorCheckStatus.FAIL,
                message =
                "Detected Java runtime feature $feature, but Microsmith requires at least " +
                    "$minSupportedJavaFeature.",
                details = mapOf("feature" to feature.toString()),
            )
        }
        return DoctorCheckResult(
            id = "java-runtime",
            status = DoctorCheckStatus.PASS,
            message = "Detected Java runtime feature $feature (minimum is $minSupportedJavaFeature).",
            details = mapOf("feature" to feature.toString()),
        )
    }

    fun checkProviderDiscovery(providerValidator: () -> List<String>): DoctorCheckResult {
        return try {
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
    }

    fun checkDirectoryWritable(id: String, directory: Path): DoctorCheckResult = runCatching {
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

    fun checkRepositoryPolicy(): DoctorCheckResult = runCatching {
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
}

private const val MIN_SUPPORTED_JAVA_FEATURE = 24
