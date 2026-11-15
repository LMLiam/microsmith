package io.github.lmliam.microsmith.gen.plugins

import io.github.lmliam.microsmith.gen.RecordingDiagnosticSink
import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PluginDiagnosticSinkTests :
    StringSpec(
        {
            "a diagnostic is forwarded with the plugin id and its own severity, code and message" {
                val recorded = RecordingDiagnosticSink()

                PluginDiagnosticSink("acme.one", recorded)
                    .report(Diagnostic(DiagnosticSeverity.WARNING, "acme.code", "careful"))

                recorded.diagnostics.map { listOf(it.pluginId, it.severity, it.code, it.message) } shouldBe
                    listOf(listOf("acme.one", DiagnosticSeverity.WARNING, "acme.code", "careful"))
            }

            "a plugin cannot report a diagnostic under another plugin id" {
                val recorded = RecordingDiagnosticSink()

                PluginDiagnosticSink("acme.one", recorded)
                    .report(Diagnostic(DiagnosticSeverity.INFO, "acme.code", "hello", pluginId = "acme.other"))

                recorded.diagnostics.single().pluginId shouldBe "acme.one"
            }
        },
    )
