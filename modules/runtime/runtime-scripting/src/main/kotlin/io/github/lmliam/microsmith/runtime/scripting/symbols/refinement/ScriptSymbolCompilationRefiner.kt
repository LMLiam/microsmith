package io.github.lmliam.microsmith.runtime.scripting.symbols.refinement

import io.github.lmliam.microsmith.runtime.scripting.symbols.discovery.ScriptSymbolContributorRegistry
import io.github.lmliam.microsmith.runtime.scripting.symbols.discovery.ScriptSymbolDiscovery
import io.github.lmliam.microsmith.runtime.scripting.symbols.model.DiscoveredScriptSymbol
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.makeFailureResult
import kotlin.script.experimental.api.providedProperties

internal fun refineMicrosmithScriptSymbols(
    context: ScriptConfigurationRefinementContext,
): ResultWithDiagnostics<ScriptCompilationConfiguration> = runCatching {
    val registry = ScriptSymbolContributorRegistry.discover()

    val discovery =
        ScriptSymbolDiscovery.discover(
            sourceName = context.script.name ?: "script.microsmith.kts",
            sourceText = context.script.text,
            registry = registry,
        )

    val symbols = discovery.symbols

    val configuration =
        ScriptCompilationConfiguration(context.compilationConfiguration) {
            if (symbols.isNotEmpty()) {
                providedProperties.append(
                    symbols.associate { symbol -> symbol.propertyName to KotlinType(symbol.definition.valueType) },
                )

                ScriptCompilationConfiguration.microsmithScriptSymbols.put(
                    symbols.map(DiscoveredScriptSymbol::compiled),
                )
            }
        }

    ResultWithDiagnostics.Success(
        configuration,
        discovery.omittedPlainNames.map { (name, alternatives) ->
            ScriptDiagnostic(
                ScriptDiagnostic.unspecifiedError,
                "'$name' is declared more than once; use ${alternatives.joinToString(" or ")}",
                ScriptDiagnostic.Severity.WARNING,
                context.script.locationId,
            )
        },
    )
}
    .fold(
        onSuccess = { it },
        onFailure = {
            makeFailureResult(
                message = it.message ?: "Failed to discover automatic Microsmith script symbols",
                path = context.script.locationId,
            )
        },
    )
