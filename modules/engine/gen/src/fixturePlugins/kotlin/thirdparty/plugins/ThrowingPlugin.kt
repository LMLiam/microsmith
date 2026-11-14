package thirdparty.plugins

import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices

class ThrowingPlugin : MicrosmithPlugin {
    override val id = "thirdparty.throwing"

    override fun services(host: HostServices): PluginServices = error("services failed on purpose")
}
