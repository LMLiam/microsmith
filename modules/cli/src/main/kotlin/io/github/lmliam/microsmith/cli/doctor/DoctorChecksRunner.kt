package io.github.lmliam.microsmith.cli.doctor

import io.github.lmliam.microsmith.cli.plugins.defaultPluginCacheDirectory
import java.nio.file.Path

internal class DoctorChecksRunner(
    private val providerValidator: () -> List<String>,
    private val scriptCacheDirectory: Path = defaultScriptCacheDirectory(),
    private val pluginCacheDirectory: Path = defaultPluginCacheDirectory(),
    private val projectRoot: Path = Path.of(".").toAbsolutePath().normalize(),
) {
    fun run(): DoctorResult {
        val checks =
            listOf(
                DoctorEnvironmentChecks.checkJavaRuntime(),
                DoctorEnvironmentChecks.checkProviderDiscovery(providerValidator),
                DoctorEnvironmentChecks.checkDirectoryWritable(id = "script-cache", directory = scriptCacheDirectory),
                DoctorEnvironmentChecks.checkDirectoryWritable(id = "plugin-cache", directory = pluginCacheDirectory),
                DoctorEnvironmentChecks.checkRepositoryPolicy(),
                DoctorBootstrapStateCheck.check(projectRoot),
            )
        return DoctorResult(checks)
    }
}

internal fun runDoctorChecks(
    providerValidator: () -> List<String>,
    scriptCacheDirectory: Path = defaultScriptCacheDirectory(),
    pluginCacheDirectory: Path = defaultPluginCacheDirectory(),
    projectRoot: Path = Path.of(".").toAbsolutePath().normalize(),
): DoctorResult = DoctorChecksRunner(
    providerValidator = providerValidator,
    scriptCacheDirectory = scriptCacheDirectory,
    pluginCacheDirectory = pluginCacheDirectory,
    projectRoot = projectRoot,
).run()
