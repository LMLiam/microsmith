package thirdparty.plugins

import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices

class CloseFailurePlugin : MicrosmithPlugin {
    override val id = "thirdparty.close-failure"

    override fun services(host: HostServices): PluginServices =
        object : PluginServices {
            override fun close() = error("close failed on purpose")
        }
}
