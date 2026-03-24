package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class GeneratedOutputWriterTests :
    StringSpec({
        "write overwrites generated files inside an existing routed output directory" {
            val workspaceRoot = Files.createTempDirectory("microsmith-generated-output-writer-")
            val outputRoot = workspaceRoot.resolve("repo-root")
            val existingProtoFile = outputRoot.resolve("services/UserService/UserCreated.proto")
            Files.createDirectories(existingProtoFile.parent)
            existingProtoFile.writeText("stale")

            GeneratedOutputWriter().write(
                outputs = listOf(
                    GeneratedFile(
                        relativePath = Path("UserCreated.proto"),
                        contents = "fresh".toByteArray(),
                        outputRoot = Path("services/UserService"),
                    ),
                ),
                space = DirectorySpace.from(outputRoot),
            )

            existingProtoFile.readText() shouldBe "fresh"
        }
    })
