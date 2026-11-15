package io.github.lmliam.microsmith.runtime.scripting.api

public interface ScriptSymbolContributor {
    public val id: String

    public val declarationCallNames: Set<String>

    public fun discover(site: ScriptSymbolDeclarationSite): ScriptSymbolDefinition?

    public fun createValue(kind: String, valueKey: String): Any
}
