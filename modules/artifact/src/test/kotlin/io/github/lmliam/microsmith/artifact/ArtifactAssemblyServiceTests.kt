package io.github.lmliam.microsmith.artifact

import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssemblyService
import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactAssembler
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class ArtifactAssemblyServiceTests :
    StringSpec({
        "assembly service merges identical text contributions into one artifact" {
            val service = ArtifactAssemblyService(listOf(TextFileArtifactAssembler()))
            val sharedId = TextFileArtifactId(relativePath = Path.of("shared.txt"))

            val assembly = service.assemble(
                listOf(
                    TextFileArtifactContribution(sharedId, "same"),
                    TextFileArtifactContribution(sharedId, "same"),
                ),
            )

            assembly.artifacts() shouldContainExactly listOf(
                TextFileArtifact(sharedId, "same"),
            )
        }

        "assembly service rejects conflicting text contribution for the same artifact" {
            val service = ArtifactAssemblyService(listOf(TextFileArtifactAssembler()))
            val sharedId = TextFileArtifactId(relativePath = Path.of("shared.txt"))

            val error = shouldThrow<IllegalArgumentException> {
                service.assemble(
                    listOf(
                        TextFileArtifactContribution(sharedId, "left"),
                        TextFileArtifactContribution(sharedId, "right"),
                    ),
                )
            }

            error.message shouldBe "Conflicting text artifact contributions for 'shared.txt' under '.'."
        }
    })
