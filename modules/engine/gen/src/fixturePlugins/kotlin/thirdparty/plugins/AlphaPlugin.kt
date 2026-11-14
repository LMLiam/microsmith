package thirdparty.plugins

import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices
import thirdparty.common.ClosingServices
import thirdparty.common.FileContributor
import thirdparty.common.FixtureResolver

class AlphaPlugin : MicrosmithPlugin {
    override val id = "thirdparty.alpha"

    override fun services(host: HostServices): PluginServices =
        object : ClosingServices(host, id) {
            override val domainResolvers = setOf(FixtureResolver())
            override val artifactContributors = setOf(object : FileContributor("alpha.txt", { "alpha:${it.text}" }) {})
        }
}
