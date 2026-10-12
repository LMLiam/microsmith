package io.github.lmliam.microsmith.runtime.scripting.api

interface ScriptSymbolContributor {
    val id: String

    val declarationCallNames: Set<String>

    fun discover(site: ScriptSymbolDeclarationSite): ScriptSymbolDefinition?

    fun createValue(kind: String, valueKey: String): Any
}
