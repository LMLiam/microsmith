package thirdparty.plugins

import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices

class DuplicatePlugin : MicrosmithPlugin {
    override val id = "thirdparty.alpha"

    override fun services(host: HostServices): PluginServices = object : PluginServices {}
}
