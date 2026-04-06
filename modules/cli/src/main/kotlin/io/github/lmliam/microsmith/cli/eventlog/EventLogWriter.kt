package io.github.lmliam.microsmith.cli.eventlog

import io.github.lmliam.microsmith.cli.diagnostics.toJsonValue
import io.github.lmliam.microsmith.cli.support.sha256IfRegularFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant

internal object EventLogWriter {
    fun writeEventLog(path: Path, event: RunEventLogEntry) {
        val payload =
            linkedMapOf(
                "timestamp" to Instant.now().toString(),
                "event" to "microsmith.run",
                "status" to event.status.wireValue,
                "exitCode" to event.exitCode,
                "scriptPath" to event.scriptPath.toAbsolutePath().normalize().toString(),
                "scriptSha256" to sha256IfRegularFile(event.scriptPath),
                "outputPath" to event.outputPath.toAbsolutePath().normalize().toString(),
                "pluginCoordinates" to event.pluginCoordinates.toList().sorted(),
                "pluginJars" to event.pluginJars.map { it.toAbsolutePath().normalize().toString() }.sorted(),
                "offline" to event.offline,
                "isolationMode" to event.isolationMode,
                "resolverStatus" to event.resolverStatus.wireValue,
                "lockfilePath" to event.lockfilePath?.toAbsolutePath()?.normalize()?.toString(),
                "cacheHit" to event.cacheHit,
                "elapsedMillis" to event.elapsedMillis,
            ).apply {
                event.failureCode?.let { put("failureCode", it.id) }
            }

        path.parent?.let(Files::createDirectories)
        Files.writeString(
            path,
            "${toJsonValue(payload)}\n",
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
            StandardOpenOption.WRITE,
        )
    }
}
