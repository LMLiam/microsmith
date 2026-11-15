package io.github.lmliam.microsmith.gen.plugins

import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import io.github.lmliam.microsmith.plugin.PluginServices

internal class OpenPlugin
private constructor(
    private val discovered: DiscoveredPlugin,
    private val services: PluginServices,
    private val diagnostics: DiagnosticSink,
) {
    val id: String
        get() = discovered.id

    fun catalog(): MicrosmithPluginCatalog =
        loadingPlugin({ discovered.description }, "reading its services") {
            MicrosmithPluginCatalog(
                domainResolvers = services.domainResolvers,
                artifactContributors = services.artifactContributors,
                artifactAssemblers = services.artifactAssemblers,
                artifactCompilers = services.artifactCompilers,
                artifactRenderers = services.artifactRenderers,
                resolutionIssueDiagnosticMappers = services.resolutionIssueDiagnosticMappers,
            )
        }

    @Suppress("TooGenericExceptionCaught")
    fun close(): Throwable? =
        try {
            services.close()
            null
        } catch (failure: Exception) {
            reportCloseFailure(failure)
        } catch (failure: LinkageError) {
            reportCloseFailure(failure)
        }

    private fun reportCloseFailure(failure: Throwable): Throwable {
        diagnostics.report(
            Diagnostic(
                severity = DiagnosticSeverity.WARNING,
                code = "gen.plugin-close-failed",
                message = "Plugin '$id' failed to close: ${failureSummary(failure)}",
            ),
        )

        return failure
    }

    companion object {
        fun open(discovered: DiscoveredPlugin, hostServices: PluginHostServicesFactory): OpenPlugin {
            val host = hostServices.create(discovered.id)
            val services =
                loadingPlugin({ discovered.description }, "creating its services") { discovered.plugin.services(host) }

            return OpenPlugin(discovered, services, host.diagnostics)
        }
    }
}
