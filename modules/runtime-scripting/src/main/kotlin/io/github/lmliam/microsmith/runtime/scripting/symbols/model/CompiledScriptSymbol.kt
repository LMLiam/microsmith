package io.github.lmliam.microsmith.runtime.scripting.symbols.model

import java.io.Serializable

internal data class CompiledScriptSymbol(
    val propertyName: String,
    val typeName: String,
    val contributorId: String,
    val kind: String,
    val valueKey: String,
) : Serializable {
    private companion object {
        const val serialVersionUID = 1L
    }
}
