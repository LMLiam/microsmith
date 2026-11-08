package io.github.lmliam.microsmith.runtime.scripting.api

data class ScriptSymbolDeclarationSite(val call: ScriptCallSite, val enclosingCalls: List<ScriptCallSite>)
