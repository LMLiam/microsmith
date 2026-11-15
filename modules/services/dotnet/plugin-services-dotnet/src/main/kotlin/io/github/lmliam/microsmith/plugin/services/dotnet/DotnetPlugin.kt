package io.github.lmliam.microsmith.plugin.services.dotnet

import dev.zacsweers.metro.createGraphFactory
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices

public class DotnetPlugin : MicrosmithPlugin {
    override val id: String = "io.github.lmliam.microsmith.dotnet"

    override fun services(host: HostServices): PluginServices =
        createGraphFactory<DotnetPluginGraph.Factory>().create().services
}
