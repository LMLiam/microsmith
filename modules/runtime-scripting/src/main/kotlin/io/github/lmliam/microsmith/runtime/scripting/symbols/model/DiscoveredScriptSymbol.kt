package io.github.lmliam.microsmith.runtime.scripting.symbols.model

import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolContributor
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolDefinition

internal data class DiscoveredScriptSymbol(
    val contributor: ScriptSymbolContributor,
    val definition: ScriptSymbolDefinition,
) {
    fun compiled(): CompiledScriptSymbol =
        CompiledScriptSymbol(
            propertyName = definition.propertyName,
            typeName =
                requireNotNull(definition.valueType.qualifiedName) {
                    "Script symbol '${definition.propertyName}' uses a local or anonymous value type"
                },
            contributorId = contributor.id,
            kind = definition.kind,
            valueKey = definition.valueKey,
        )
}
