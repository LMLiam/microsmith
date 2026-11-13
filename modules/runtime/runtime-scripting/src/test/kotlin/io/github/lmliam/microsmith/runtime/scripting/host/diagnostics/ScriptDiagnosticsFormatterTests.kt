package io.github.lmliam.microsmith.runtime.scripting.host.diagnostics

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.script.experimental.api.ScriptDiagnostic

class ScriptDiagnosticsFormatterTests :
    StringSpec({
        "omits debug and info reports" {
            val reports =
                listOf(
                    ScriptDiagnostic(0, "Loading modules", ScriptDiagnostic.Severity.DEBUG),
                    ScriptDiagnostic(0, "Resolved classpath", ScriptDiagnostic.Severity.INFO),
                    ScriptDiagnostic(0, "Deprecated call", ScriptDiagnostic.Severity.WARNING),
                    ScriptDiagnostic(0, "Unresolved reference", ScriptDiagnostic.Severity.ERROR),
                )

            ScriptDiagnosticsFormatter.format(reports)
                .shouldContainExactly(
                    "[error] <script> Unresolved reference",
                    "[warning] <script> Deprecated call",
                )
        }
    })
