package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.gen.plugins.MicrosmithPluginCatalog
import io.github.lmliam.microsmith.gen.plugins.discoverMicrosmithPluginIds
import io.github.lmliam.microsmith.plugin.schemas.protobuf.ProtobufPlugin
import io.github.lmliam.microsmith.plugin.schemas.protobuf.rpc.ProtobufRpcPlugin
import io.github.lmliam.microsmith.plugin.services.dotnet.DotnetPlugin
import io.github.lmliam.microsmith.plugin.services.dotnet.asp.DotnetAspPlugin
import io.github.lmliam.microsmith.plugin.services.dotnet.packages.DotnetPackagesPlugin

internal val builtinPluginIds: List<String> =
    listOf(DotnetPlugin(), DotnetAspPlugin(), DotnetPackagesPlugin(), ProtobufPlugin(), ProtobufRpcPlugin()).map {
        it.id
    }

internal fun verifyBuiltinProviders(): List<String> =
    verifyBuiltinProviders(discoverMicrosmithPluginIds(MicrosmithPluginCatalog::class.java.classLoader))

internal fun verifyBuiltinProviders(
    pluginIds: Set<String>,
    requiredPluginIds: List<String> = builtinPluginIds,
): List<String> =
    requiredPluginIds.filterNot(pluginIds::contains).map {
        "Missing built-in plugin '$it'. Check CLI runtime packaging."
    }
