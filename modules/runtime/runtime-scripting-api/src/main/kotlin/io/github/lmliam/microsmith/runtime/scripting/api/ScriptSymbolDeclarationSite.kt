package io.github.lmliam.microsmith.runtime.scripting.api

public data class ScriptSymbolDeclarationSite(
    public val call: ScriptCallSite,
    public val enclosingCalls: List<ScriptCallSite>,
)
