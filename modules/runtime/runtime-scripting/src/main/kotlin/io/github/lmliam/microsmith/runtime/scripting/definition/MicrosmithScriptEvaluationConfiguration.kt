package io.github.lmliam.microsmith.runtime.scripting.definition

import io.github.lmliam.microsmith.runtime.scripting.symbols.refinement.refineMicrosmithScriptSymbolValues
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.refineConfigurationBeforeEvaluate

internal object MicrosmithScriptEvaluationConfiguration :
    ScriptEvaluationConfiguration({ refineConfigurationBeforeEvaluate(::refineMicrosmithScriptSymbolValues) })
