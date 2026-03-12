package me.liam.microsmith.runtime.scripting.host

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunSuccess
import kotlin.io.path.createTempDirectory
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ScriptEvaluationConfiguration

class ScriptEvaluationSuccessFinalizerTests :
    StringSpec({
        "returns success and auto-emits a returned model when nothing was emitted explicitly" {
            val emittedModels = mutableListOf<MicrosmithModel>()
            val scriptContext =
                MicrosmithScriptContext(
                    outDir = createTempDirectory("microsmith-script-eval-success"),
                    vars = emptyMap(),
                    flags = emptySet(),
                    emitHandler = emittedModels::add,
                )
            val evaluationResult =
                EvaluationResult(
                    returnValue =
                    ResultValue.Value(
                        "result",
                        MicrosmithModel.empty(),
                        "MicrosmithModel",
                        null,
                        null,
                    ),
                    configuration = ScriptEvaluationConfiguration {},
                )

            val result =
                ScriptEvaluationSuccessFinalizer().complete(
                    evaluationResult = evaluationResult,
                    scriptContext = scriptContext,
                    warnings = listOf("warning"),
                    cacheHit = true,
                    elapsedMillis = 42,
                ).shouldBeTypeOf<ScriptRunSuccess>()

            emittedModels.size shouldBe 1
            result.warnings shouldBe listOf("warning")
            result.cacheHit shouldBe true
            result.elapsedMillis shouldBe 42
        }

        "returns evaluation failure when no model is returned or emitted" {
            val scriptContext =
                MicrosmithScriptContext(
                    outDir = createTempDirectory("microsmith-script-eval-failure"),
                    vars = emptyMap(),
                    flags = emptySet(),
                    emitHandler = { error("emit should not be called") },
                )
            val evaluationResult =
                EvaluationResult(
                    returnValue = ResultValue.Unit(Any::class, Any()),
                    configuration = ScriptEvaluationConfiguration {},
                )

            val result =
                ScriptEvaluationSuccessFinalizer().complete(
                    evaluationResult = evaluationResult,
                    scriptContext = scriptContext,
                    warnings = listOf("warning"),
                    cacheHit = false,
                    elapsedMillis = 7,
                ).shouldBeTypeOf<ScriptRunFailure>()

            result.type shouldBe ScriptFailureType.EVALUATION
            result.diagnostics.shouldContain("warning")
            result.diagnostics.shouldContain(
                "Script evaluation failed: Script must either return MicrosmithModel or call " +
                    "emit(model)/generate(model).",
            )
        }
    })
