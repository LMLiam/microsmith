package io.github.lmliam.microsmith.runtime.scripting.symbols.refinement

import io.github.lmliam.microsmith.runtime.scripting.symbols.discovery.ScriptSymbolContributorRegistry
import io.github.lmliam.microsmith.runtime.scripting.symbols.discovery.ScriptSymbolDiscovery
import io.github.lmliam.microsmith.runtime.scripting.symbols.model.DiscoveredScriptSymbol
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.makeFailureResult
import kotlin.script.experimental.api.providedProperties

internal fun refineMicrosmithScriptSymbols(
    context: ScriptConfigurationRefinementContext
): ResultWithDiagnostics<ScriptCompilationConfiguration> = runCatching {
    val registry = ScriptSymbolContributorRegistry.discover()

    val symbols =
        ScriptSymbolDiscovery.discover(
            sourceName = context.script.name ?: "script.microsmith.kts",
            sourceText = context.script.text,
            registry = registry,
        )

    ScriptCompilationConfiguration(context.compilationConfiguration) {
        if (symbols.isNotEmpty()) {
            providedProperties.append(
                symbols.associate { symbol ->
                    symbol.definition.propertyName to KotlinType(symbol.definition.valueType)
                }
            )

            ScriptCompilationConfiguration.microsmithScriptSymbols.put(symbols.map(DiscoveredScriptSymbol::compiled))
        }
    }
}
    .fold(
        onSuccess = { it.asSuccess() },
        onFailure = {
            makeFailureResult(
                message = it.message ?: "Failed to discover automatic Microsmith script symbols",
                path = context.script.locationId,
            )
        },
    )
