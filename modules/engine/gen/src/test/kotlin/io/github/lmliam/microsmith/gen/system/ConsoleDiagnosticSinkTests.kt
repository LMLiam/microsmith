package io.github.lmliam.microsmith.gen.system

import io.github.lmliam.microsmith.plugin.Console
import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private class RecordingConsole : Console {
    val outLines = mutableListOf<String>()
    val errLines = mutableListOf<String>()

    override fun out(line: String) {
        outLines += line
    }

    override fun err(line: String) {
        errLines += line
    }
}

class ConsoleDiagnosticSinkTests :
    StringSpec(
        {
            "an info diagnostic is written to the standard output" {
                val console = RecordingConsole()

                ConsoleDiagnosticSink(console).report(Diagnostic(DiagnosticSeverity.INFO, "test.info", "progress"))

                console.outLines shouldBe listOf("progress")
                console.errLines shouldBe emptyList()
            }

            "a warning diagnostic is written to the standard error" {
                val console = RecordingConsole()

                ConsoleDiagnosticSink(console).report(Diagnostic(DiagnosticSeverity.WARNING, "test.warning", "careful"))

                console.errLines shouldBe listOf("careful")
                console.outLines shouldBe emptyList()
            }

            "a diagnostic from a plugin is prefixed with the plugin id" {
                val console = RecordingConsole()

                ConsoleDiagnosticSink(console)
                    .report(Diagnostic(DiagnosticSeverity.WARNING, "test.warning", "careful", pluginId = "acme.plugin"))

                console.errLines shouldBe listOf("[acme.plugin] careful")
            }
        },
    )
