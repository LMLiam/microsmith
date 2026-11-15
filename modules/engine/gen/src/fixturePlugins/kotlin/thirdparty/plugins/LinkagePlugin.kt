package thirdparty.plugins

import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices
import thirdparty.linkagemissing.Absent

class LinkagePlugin : MicrosmithPlugin {
    override val id = "thirdparty.linkage"

    override fun services(host: HostServices): PluginServices = Absent.services()
}
