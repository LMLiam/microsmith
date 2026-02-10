package me.liam.microsmith.runtime.scripting

import kotlin.script.experimental.api.ScriptDiagnostic

internal object ScriptDiagnosticsFormatter {
    fun format(reports: List<ScriptDiagnostic>): List<String> =
        reports
            .sortedWith(
                compareByDescending<ScriptDiagnostic> { it.severity }
                    .thenBy { it.sourcePath.orEmpty() }
                    .thenBy { it.location?.start?.line ?: Int.MAX_VALUE }
                    .thenBy { it.location?.start?.col ?: Int.MAX_VALUE }
                    .thenBy { it.message }
            ).map { report ->
                val severity = report.severity.name.lowercase()
                val source = report.sourcePath?.let(::shortPath) ?: "<script>"
                val line = report.location?.start?.line
                val column = report.location?.start?.col
                val locationSuffix =
                    if (line != null && column != null) {
                        "$source:$line:$column"
                    } else {
                        source
                    }
                "[$severity] $locationSuffix ${report.message}"
            }

    fun containsErrors(lines: List<String>): Boolean =
        lines.any { line ->
            line.startsWith("[error]") || line.startsWith("[fatal]")
        }

    private fun shortPath(path: String): String = path.substringAfterLast('/').substringAfterLast('\\')
}
