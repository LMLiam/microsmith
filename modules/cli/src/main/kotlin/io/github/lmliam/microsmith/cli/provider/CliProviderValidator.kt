package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.gen.plugins.MicrosmithPluginCatalog
import io.github.lmliam.microsmith.gen.plugins.discoverMicrosmithPlugins

internal fun verifyBuiltinProviders(): List<String> = verifyBuiltinProviders(discoverMicrosmithPlugins())

internal fun verifyBuiltinProviders(pluginCatalog: MicrosmithPluginCatalog): List<String> =
    missingBuiltinProviderMessages(pluginCatalog)
