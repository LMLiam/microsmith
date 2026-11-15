package thirdparty.common

import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.PluginServices
import thirdparty.contract.CLOSED_CODE

open class ClosingServices(private val host: HostServices, private val id: String) : PluginServices {
    override fun close() {
        host.diagnostics.report(Diagnostic(DiagnosticSeverity.INFO, CLOSED_CODE, id))
    }
}
