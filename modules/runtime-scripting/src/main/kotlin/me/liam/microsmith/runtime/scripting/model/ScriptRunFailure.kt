package me.liam.microsmith.runtime.scripting.model

data class ScriptRunFailure(val diagnostics: List<String>, val type: ScriptFailureType = ScriptFailureType.HOST) :
    ScriptRunResult
