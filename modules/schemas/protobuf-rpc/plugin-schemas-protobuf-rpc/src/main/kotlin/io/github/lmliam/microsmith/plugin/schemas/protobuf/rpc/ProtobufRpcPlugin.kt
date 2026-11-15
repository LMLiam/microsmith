package io.github.lmliam.microsmith.plugin.schemas.protobuf.rpc

import dev.zacsweers.metro.createGraphFactory
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices

class ProtobufRpcPlugin : MicrosmithPlugin {
    override val id: String = "io.github.lmliam.microsmith.protobuf.rpc"

    override fun services(host: HostServices): PluginServices =
        createGraphFactory<ProtobufRpcPluginGraph.Factory>().create().services
}
