package thirdparty.plugins

import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices
import thirdparty.common.ClosingServices
import thirdparty.common.FileContributor

class ConflictingPlugin : MicrosmithPlugin {
    override val id = "thirdparty.conflicting"

    override fun services(host: HostServices): PluginServices =
        object : ClosingServices(host, id) {
            override val artifactContributors = setOf(object : FileContributor("alpha.txt", { "conflicting" }) {})
        }
}
