package io.github.lmliam.microsmith.plugin

public interface DiagnosticSink {
    public fun report(diagnostic: Diagnostic)
}
