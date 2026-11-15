package io.github.lmliam.microsmith.runtime.scripting.symbols.discovery

import io.github.lmliam.microsmith.runtime.scripting.symbols.model.DiscoveredScriptSymbol

internal data class ScriptSymbolDiscoveryResult(
    val symbols: List<DiscoveredScriptSymbol>,
    val omittedPlainNames: Map<String, List<String>>,
)
