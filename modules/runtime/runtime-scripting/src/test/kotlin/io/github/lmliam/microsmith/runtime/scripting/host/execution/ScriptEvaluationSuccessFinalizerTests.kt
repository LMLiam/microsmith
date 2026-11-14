package io.github.lmliam.microsmith.runtime.scripting.host.execution

import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.gen.errors.GenerationResolutionFailedException
import io.github.lmliam.microsmith.resolve.ResolutionIssue
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunDiagnostic
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlin.io.path.createTempDirectory
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ScriptEvaluationConfiguration

private data object TestIssue : ResolutionIssue

class ScriptEvaluationSuccessFinalizerTests :
    StringSpec({
        "returns success and auto-emits a returned model when nothing was emitted explicitly" {
            val emittedModels = mutableListOf<MicrosmithModel>()
            val generatedRoot = createTempDirectory("microsmith-script-generated-root")
            val scriptContext =
                MicrosmithScriptContext(
                    outDir = createTempDirectory("microsmith-script-eval-success"),
                    vars = emptyMap(),
                    flags = emptySet(),
                    emitHandler = { model ->
                        emittedModels.add(model)
                        listOf(generatedRoot)
                    },
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
                ScriptEvaluationSuccessFinalizer()
                    .complete(
                        evaluationResult = evaluationResult,
                        scriptContext = scriptContext,
                        warnings = listOf("warning"),
                        cacheHit = true,
                        elapsedMillis = 42,
                    )
                    .shouldBeTypeOf<ScriptRunSuccess>()

            emittedModels.size shouldBe 1
            result.warnings shouldBe listOf("warning")
            result.cacheHit shouldBe true
            result.elapsedMillis shouldBe 42
            result.generatedRoots.shouldContainExactly(listOf(generatedRoot))
        }

        "returns success with every generated root when a script emits multiple models" {
            val generatedRootOne = createTempDirectory("microsmith-script-generated-root-one")
            val generatedRootTwo = createTempDirectory("microsmith-script-generated-root-two")
            val emittedRoots = ArrayDeque(listOf(generatedRootOne, generatedRootTwo))
            val scriptContext =
                MicrosmithScriptContext(
                    outDir = createTempDirectory("microsmith-script-eval-multi-emit"),
                    vars = emptyMap(),
                    flags = emptySet(),
                    emitHandler = { listOf(emittedRoots.removeFirst()) },
                )
            scriptContext.emit(MicrosmithModel.empty())
            scriptContext.generate(MicrosmithModel.empty())
            val evaluationResult =
                EvaluationResult(
                    returnValue = ResultValue.Unit(Any::class, Any()),
                    configuration = ScriptEvaluationConfiguration {},
                )

            val result =
                ScriptEvaluationSuccessFinalizer()
                    .complete(
                        evaluationResult = evaluationResult,
                        scriptContext = scriptContext,
                        warnings = emptyList(),
                        cacheHit = false,
                        elapsedMillis = 9,
                    )
                    .shouldBeTypeOf<ScriptRunSuccess>()

            result.generatedRoots.shouldContainExactly(listOf(generatedRootOne, generatedRootTwo).sorted())
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
                ScriptEvaluationSuccessFinalizer()
                    .complete(
                        evaluationResult = evaluationResult,
                        scriptContext = scriptContext,
                        warnings = listOf("warning"),
                        cacheHit = false,
                        elapsedMillis = 7,
                    )
                    .shouldBeTypeOf<ScriptRunFailure>()

            result.type shouldBe ScriptFailureType.EVALUATION
            val messages = result.diagnostics.map { it.message }
            messages.shouldContain("warning")
            messages.shouldContain(
                "Script evaluation failed: Script must either return MicrosmithModel or call " +
                    "emit(model)/generate(model)."
            )
        }

        "reports each resolution issue as its own coded diagnostic after the script warnings" {
            val scriptContext =
                MicrosmithScriptContext(
                    outDir = createTempDirectory("microsmith-script-resolution-failure"),
                    vars = emptyMap(),
                    flags = emptySet(),
                    emitHandler = { error("emit should not be called") },
                )
            val resolutionFailure =
                GenerationResolutionFailedException(
                    issues = nonEmptyListOf(TestIssue, TestIssue),
                    diagnostics =
                        nonEmptyListOf(
                            ResolutionDiagnostic(code = "protobuf.first", message = "first problem"),
                            ResolutionDiagnostic(code = "protobuf.second", message = "second problem"),
                        ),
                )
            val evaluationResult =
                EvaluationResult(
                    returnValue =
                        ResultValue.Error(
                            error = IllegalStateException("script failed", resolutionFailure),
                            wrappingException = null,
                        ),
                    configuration = ScriptEvaluationConfiguration {},
                )

            val result =
                ScriptEvaluationSuccessFinalizer()
                    .complete(
                        evaluationResult = evaluationResult,
                        scriptContext = scriptContext,
                        warnings = listOf("[warning] script.kts:1:1 deprecated"),
                        cacheHit = false,
                        elapsedMillis = 7,
                    )
                    .shouldBeTypeOf<ScriptRunFailure>()

            result.type shouldBe ScriptFailureType.RESOLUTION
            result.diagnostics.shouldContainExactly(
                ScriptRunDiagnostic(message = "[warning] script.kts:1:1 deprecated"),
                ScriptRunDiagnostic(message = "first problem", code = "protobuf.first"),
                ScriptRunDiagnostic(message = "second problem", code = "protobuf.second"),
            )
        }
    })
