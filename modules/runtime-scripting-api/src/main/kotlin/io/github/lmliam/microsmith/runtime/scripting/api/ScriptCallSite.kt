package io.github.lmliam.microsmith.runtime.scripting.api

data class ScriptCallSite(val callee: ScriptCallCallee, val arguments: List<ScriptLiteral?>)
