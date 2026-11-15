package io.github.lmliam.microsmith.runtime.scripting.model

import java.nio.file.Path

public data class ScriptRunRequest(
    public val script: Path,
    public val outputDir: Path,
    public val variables: Map<String, String>,
    public val flags: Set<String>,
    public val pluginClasspath: List<Path> = emptyList(),
    public val isolationMode: ScriptIsolationMode = ScriptIsolationMode.CLASSLOADER,
)
