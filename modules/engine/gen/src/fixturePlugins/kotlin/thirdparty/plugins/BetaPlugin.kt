package thirdparty.plugins

import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices
import thirdparty.common.ClosingServices
import thirdparty.common.FileContributor

class BetaPlugin : MicrosmithPlugin {
    override val id = "thirdparty.beta"

    override fun services(host: HostServices): PluginServices =
        object : ClosingServices(host, id) {
            override val artifactContributors = setOf(object : FileContributor("beta.txt", { "beta:${it.text}" }) {})
        }
}
