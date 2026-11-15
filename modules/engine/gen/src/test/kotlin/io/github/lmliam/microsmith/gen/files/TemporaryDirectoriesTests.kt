package io.github.lmliam.microsmith.gen.files

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.github.lmliam.microsmith.gen.RecordingDiagnosticSink
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.Environment
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.nio.file.FileSystem
import java.nio.file.Files
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.test.UnconfinedTestDispatcher

private class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

private class FixedEnvironment(private val properties: Map<String, String>) : Environment {
    override fun variable(name: String): String? = null

    override fun property(name: String): String? = properties[name]
}

class TemporaryDirectoriesTests :
    StringSpec(
        {
            fun temporaryDirectories(fileSystem: FileSystem, diagnostics: RecordingDiagnosticSink) =
                TemporaryDirectories(
                    fileSystem = fileSystem,
                    clock = FixedClock(Instant.fromEpochMilliseconds(1_234)),
                    environment = FixedEnvironment(mapOf("java.io.tmpdir" to "/tmp")),
                    diagnostics = diagnostics,
                    ioDispatcher = UnconfinedTestDispatcher(),
                )

            "create makes a directory under the temporary directory named from the clock" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    Files.createDirectories(fileSystem.getPath("/tmp"))

                    val directory = temporaryDirectories(fileSystem, RecordingDiagnosticSink()).create()

                    directory.root.parent shouldBe fileSystem.getPath("/tmp")
                    directory.root.fileName.toString() shouldStartWith ".microsmith-gen-temp-1234"
                    Files.isDirectory(directory.root) shouldBe true
                }
            }

            "close deletes the directory and its contents" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    Files.createDirectories(fileSystem.getPath("/tmp"))
                    val directory = temporaryDirectories(fileSystem, RecordingDiagnosticSink()).create()
                    Files.createDirectories(directory.root.resolve("nested"))
                    Files.writeString(directory.root.resolve("nested/file.txt"), "contents")

                    directory.close()

                    Files.exists(directory.root) shouldBe false
                }
            }

            "close reports a warning when the directory cannot be cleaned up" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    Files.createDirectories(fileSystem.getPath("/tmp"))
                    val diagnostics = RecordingDiagnosticSink()
                    val directory = temporaryDirectories(fileSystem, diagnostics).create()
                    Files.delete(directory.root)

                    directory.close()

                    diagnostics.diagnostics.map { it.severity } shouldBe listOf(DiagnosticSeverity.WARNING)
                    diagnostics.diagnostics.single().message shouldStartWith
                        "Failed to cleanup temporary directory '${directory.root}'"
                }
            }
        },
    )
