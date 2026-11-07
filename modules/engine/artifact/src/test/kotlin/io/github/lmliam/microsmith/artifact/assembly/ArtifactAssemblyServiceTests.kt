package io.github.lmliam.microsmith.artifact.assembly

import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactAssembler
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ArtifactAssemblyServiceTests :
    StringSpec({

        val service = ArtifactAssemblyService(listOf(TextFileArtifactAssembler()))

        "merges identical text contributions into a single artifact" {
            val sharedId = TextFileArtifactId("shared.txt")

            val assembly =
                service.assemble(
                    listOf(
                        TextFileArtifactContribution(sharedId, "same"),
                        TextFileArtifactContribution(sharedId, "same"),
                    ),
                )

            assembly.artifacts() shouldContainExactly
                listOf(
                    TextFileArtifact(sharedId, "same"),
                )
        }

        "rejects conflicting text contributions for the same artifact ID" {
            val sharedId = TextFileArtifactId("shared.txt")

            val error =
                shouldThrow<IllegalArgumentException> {
                    service.assemble(
                        listOf(
                            TextFileArtifactContribution(sharedId, "left"),
                            TextFileArtifactContribution(sharedId, "right"),
                        ),
                    )
                }

            error.message shouldBe "Conflicting text artifact contributions for 'shared.txt' under '.'."
        }

        "merges contribution into an existing retained artifact" {
            val sharedId = TextFileArtifactId("shared.txt")
            val retained = TextFileArtifact(sharedId, "content")

            val assembly =
                service.assemble(
                    contributions = listOf(TextFileArtifactContribution(sharedId, "content")),
                    retainedArtifacts = listOf(retained),
                )

            assembly.artifacts() shouldContainExactly listOf(retained)
        }

        "rejects conflicting retained artifacts with identical IDs" {
            val sharedId = TextFileArtifactId("shared.txt")
            val retainedA = TextFileArtifact(sharedId, "versionA")
            val retainedB = TextFileArtifact(sharedId, "versionB")

            val error =
                shouldThrow<IllegalArgumentException> {
                    service.assemble(
                        contributions = emptyList(),
                        retainedArtifacts = listOf(retainedA, retainedB),
                    )
                }

            error.message shouldBe
                "Conflicting retained artifact for 'TextFileArtifactId(relativePath=shared.txt, outputRoot=.)'."
        }
    })
