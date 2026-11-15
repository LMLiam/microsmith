package io.github.lmliam.microsmith.runtime.scripting.api

public data class ScriptCallSite(public val callee: ScriptCallCallee, public val arguments: List<ScriptLiteral?>)
