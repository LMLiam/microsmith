package io.github.lmliam.microsmith.plugin.services.dotnet.packages

import dev.zacsweers.metro.createGraphFactory
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices

public class DotnetPackagesPlugin : MicrosmithPlugin {
    override val id: String = "io.github.lmliam.microsmith.dotnet.packages"

    override fun services(host: HostServices): PluginServices =
        createGraphFactory<DotnetPackagesPluginGraph.Factory>().create().services
}
