package io.github.lmliam.microsmith.gen.execution

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.github.lmliam.microsmith.gen.RecordingDiagnosticSink
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GenerationProgressReporterTests :
    StringSpec(
        {
            "completion is reported as an info diagnostic that names the output root" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val diagnostics = RecordingDiagnosticSink()

                    GenerationProgressReporter(diagnostics)
                        .reportModelGenerationComplete(DirectorySpace(fileSystem.getPath("/out")))

                    val reported = diagnostics.diagnostics.single()
                    reported.severity shouldBe DiagnosticSeverity.INFO
                    reported.message shouldBe "Generated all files in /out"
                }
            }
        },
    )
