package io.github.lmliam.microsmith.runtime.scripting.symbols.refinement

import io.github.lmliam.microsmith.runtime.scripting.symbols.discovery.ScriptSymbolContributorRegistry
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfigurationRefinementContext
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.compilationConfiguration
import kotlin.script.experimental.api.makeFailureResult
import kotlin.script.experimental.api.providedProperties

internal fun refineMicrosmithScriptSymbolValues(
    context: ScriptEvaluationConfigurationRefinementContext,
): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val compilationConfiguration =
        context.evaluationConfiguration[ScriptEvaluationConfiguration.compilationConfiguration]
            ?: return context.evaluationConfiguration.asSuccess()

    val symbols = compilationConfiguration[ScriptCompilationConfiguration.microsmithScriptSymbols].orEmpty()

    if (symbols.isEmpty()) {
        return context.evaluationConfiguration.asSuccess()
    }

    return runCatching {
        val registry = ScriptSymbolContributorRegistry.discover()

        val values = symbols.associate { symbol ->
            val contributor = registry.contributor(symbol.contributorId)

            symbol.propertyName to contributor.createValue(symbol.kind, symbol.valueKey)
        }

        ScriptEvaluationConfiguration(context.evaluationConfiguration) {
            providedProperties.append(values)
        }
    }.fold(
        onSuccess = { it.asSuccess() },
        onFailure = {
            makeFailureResult(
                it.message ?: "Failed to create automatic Microsmith script symbol values",
            )
        },
    )
}
