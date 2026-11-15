package io.github.lmliam.microsmith.plugin.schemas.protobuf

import dev.zacsweers.metro.createGraphFactory
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices

public class ProtobufPlugin : MicrosmithPlugin {
    override val id: String = "io.github.lmliam.microsmith.protobuf"

    override fun services(host: HostServices): PluginServices =
        createGraphFactory<ProtobufPluginGraph.Factory>().create().services
}
