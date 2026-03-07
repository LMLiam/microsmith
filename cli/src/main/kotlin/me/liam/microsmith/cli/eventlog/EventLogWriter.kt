package me.liam.microsmith.cli.eventlog

import me.liam.microsmith.cli.diagnostics.toJsonValue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
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
                "scriptSha256" to sha256IfPresent(event.scriptPath),
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

    private fun sha256IfPresent(path: Path): String? {
        val normalized = path.toAbsolutePath().normalize()
        if (!Files.exists(normalized) || !Files.isRegularFile(normalized)) {
            return null
        }

        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(normalized).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { "%02x".format(it) }
    }
}
