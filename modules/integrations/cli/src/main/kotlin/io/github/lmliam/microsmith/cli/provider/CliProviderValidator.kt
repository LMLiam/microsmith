package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.gen.plugins.MicrosmithPluginCatalog
import io.github.lmliam.microsmith.gen.plugins.discoverMicrosmithPlugins

internal fun verifyBuiltinProviders(): List<String> =
    verifyBuiltinProviders(discoverMicrosmithPlugins(MicrosmithPluginCatalog::class.java.classLoader))

internal fun verifyBuiltinProviders(pluginCatalog: MicrosmithPluginCatalog): List<String> =
    missingBuiltinProviderMessages(pluginCatalog)
