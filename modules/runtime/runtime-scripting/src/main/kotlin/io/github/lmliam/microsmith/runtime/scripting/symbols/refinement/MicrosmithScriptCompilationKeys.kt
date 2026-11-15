package io.github.lmliam.microsmith.runtime.scripting.symbols.refinement

import io.github.lmliam.microsmith.runtime.scripting.symbols.discovery.ScriptSymbolContributorRegistry
import io.github.lmliam.microsmith.runtime.scripting.symbols.model.CompiledScriptSymbol
import kotlin.script.experimental.api.ScriptCompilationConfigurationKeys
import kotlin.script.experimental.host.ScriptingHostConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

internal val ScriptCompilationConfigurationKeys.microsmithScriptSymbols by
    PropertiesCollection.key<List<CompiledScriptSymbol>>()

internal val ScriptingHostConfigurationKeys.microsmithScriptSymbolRegistry by
    PropertiesCollection.key<ScriptSymbolContributorRegistry>(isTransient = true)
