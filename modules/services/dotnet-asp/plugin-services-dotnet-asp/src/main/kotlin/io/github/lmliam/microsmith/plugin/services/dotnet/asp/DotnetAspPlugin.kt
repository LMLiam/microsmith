package io.github.lmliam.microsmith.plugin.services.dotnet.asp

import dev.zacsweers.metro.createGraphFactory
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices

public class DotnetAspPlugin : MicrosmithPlugin {
    override val id: String = "io.github.lmliam.microsmith.dotnet.asp"

    override fun services(host: HostServices): PluginServices =
        createGraphFactory<DotnetAspPluginGraph.Factory>().create().services
}
