package io.github.lmliam.microsmith.plugin

interface DiagnosticSink {
    fun report(diagnostic: Diagnostic)
}
