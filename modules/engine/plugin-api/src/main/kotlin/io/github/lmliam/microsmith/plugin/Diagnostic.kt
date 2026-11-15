package io.github.lmliam.microsmith.plugin

public class Diagnostic(
    public val severity: DiagnosticSeverity,
    public val code: String,
    public val message: String,
    public val pluginId: String? = null,
)
