package me.liam.microsmith.cli.eventlog

import me.liam.microsmith.cli.diagnostics.CliFailureCode
import me.liam.microsmith.cli.execution.RunExecutionStatus
import java.nio.file.Path

internal data class RunEventLogEntry(
    val scriptPath: Path,
    val outputPath: Path,
    val pluginCoordinates: Set<String>,
    val pluginJars: Set<Path>,
    val offline: Boolean,
    val isolationMode: String,
    val status: RunExecutionStatus,
    val exitCode: Int,
    val failureCode: CliFailureCode? = null,
    val resolverStatus: RunExecutionStatus,
    val lockfilePath: Path? = null,
    val cacheHit: Boolean? = null,
    val elapsedMillis: Long? = null,
)
