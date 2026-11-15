package thirdparty.plugins

import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices
import thirdparty.common.ClosingServices
import thirdparty.common.FileContributor
import thirdparty.contract.CONTRIBUTING_CODE

class BetaPlugin : MicrosmithPlugin {
    override val id = "thirdparty.beta"

    override fun services(host: HostServices): PluginServices =
        object : ClosingServices(host, id) {
            override val artifactContributors =
                setOf(
                    object :
                        FileContributor(
                            "beta.txt",
                            { model ->
                                host.diagnostics.report(Diagnostic(DiagnosticSeverity.INFO, CONTRIBUTING_CODE, id))
                                "beta:${model.text}"
                            },
                        ) {},
                )
        }
}
