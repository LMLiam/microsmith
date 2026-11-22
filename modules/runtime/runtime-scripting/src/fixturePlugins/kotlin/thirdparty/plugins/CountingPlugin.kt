package thirdparty.plugins

import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices
import thirdparty.contract.GENERATION_RUN_OPENED_CODE

class CountingPlugin : MicrosmithPlugin {
    override val id = "thirdparty.counting"

    override fun services(host: HostServices): PluginServices {
        host.diagnostics.report(Diagnostic(DiagnosticSeverity.INFO, GENERATION_RUN_OPENED_CODE, id))
        return object : PluginServices {}
    }
}
