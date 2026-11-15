package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.gen.plugins.MicrosmithPluginCatalog
import io.github.lmliam.microsmith.gen.plugins.discoverMicrosmithPluginIds
import io.github.lmliam.microsmith.gen.plugins.discoverMicrosmithPlugins

internal fun verifyBuiltinProviders(): List<String> {
    val classLoader = MicrosmithPluginCatalog::class.java.classLoader

    return verifyBuiltinProviders(discoverMicrosmithPlugins(classLoader), discoverMicrosmithPluginIds(classLoader))
}

internal fun verifyBuiltinProviders(
    pluginCatalog: MicrosmithPluginCatalog,
    pluginIds: Set<String> = emptySet(),
    requiredPluginIds: List<String> = builtinPluginIds,
): List<String> =
    missingBuiltinProviderMessages(pluginCatalog) + missingBuiltinPluginMessages(pluginIds, requiredPluginIds)
