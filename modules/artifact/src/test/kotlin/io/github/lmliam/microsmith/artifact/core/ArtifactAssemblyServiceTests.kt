package io.github.lmliam.microsmith.artifact.core

import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactAssembler
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.resolve.core.ResolvedModel
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.nio.file.Path

private data class AlphaResolved(val name: String) : ResolvedModel

private data class BetaResolved(val name: String) : ResolvedModel

private class AlphaContributor : ArtifactContributor<AlphaResolved> {
    override val resolvedType = AlphaResolved::class

    override fun contribute(model: AlphaResolved): List<ArtifactContribution<out Artifact>> = listOf(
        TextFileArtifactContribution(
            artifactId = TextFileArtifactId(relativePath = Path.of("alpha.txt")),
            contents = model.name,
        ),
    )
}

private class BetaContributor : ArtifactContributor<BetaResolved> {
    override val resolvedType = BetaResolved::class

    override fun contribute(model: BetaResolved): List<ArtifactContribution<out Artifact>> = listOf(
        TextFileArtifactContribution(
            artifactId = TextFileArtifactId(relativePath = Path.of("beta.txt")),
            contents = model.name,
        ),
    )
}

@Suppress("UNCHECKED_CAST")
private class MisdeclaredContributor : ArtifactContributor<AlphaResolved> {
    override val resolvedType = BetaResolved::class as kotlin.reflect.KClass<AlphaResolved>

    override fun contribute(model: AlphaResolved): List<ArtifactContribution<out Artifact>> = emptyList()
}

class ArtifactAssemblyServiceTests :
    StringSpec({
        "contribution service resolves contributors by resolved model type in deterministic order" {
            val service =
                ArtifactContributionService(
                    ArtifactContributorRegistry(
                        listOf(
                            BetaContributor(),
                            AlphaContributor(),
                        ),
                    ),
                )

            val contributions = service.contribute(listOf(BetaResolved("second"), AlphaResolved("first")))

            contributions
                .map { (it as TextFileArtifactContribution).artifactId.relativePath.toString() } shouldContainExactly
                listOf(
                    "alpha.txt",
                    "beta.txt",
                )
        }

        "contributor registry rejects registrations whose declared resolvedType does not match the generic contract" {
            val error =
                shouldThrow<IllegalArgumentException> {
                    ArtifactContributorRegistry(listOf(MisdeclaredContributor()))
                }

            error.message shouldBe
                "io.github.lmliam.microsmith.artifact.core.MisdeclaredContributor declares " +
                "resolvedType io.github.lmliam.microsmith.artifact.core.BetaResolved, but implements " +
                "ArtifactContributor<io.github.lmliam.microsmith.artifact.core.AlphaResolved>. " +
                "Ensure resolvedType matches the ArtifactContributor<R> generic type."
        }

        "assembly service merges identical text contributions and rejects conflicting ones" {
            val assemblyService =
                ArtifactAssemblyService(
                    ArtifactAssemblerRegistry(listOf(TextFileArtifactAssembler())),
                )
            val sharedId = TextFileArtifactId(relativePath = Path.of("shared.txt"))

            val assembly =
                assemblyService.assemble(
                    listOf(
                        TextFileArtifactContribution(sharedId, "same"),
                        TextFileArtifactContribution(sharedId, "same"),
                    ),
                )

            assembly.artifacts() shouldContainExactly listOf(TextFileArtifact(sharedId, "same"))

            shouldThrow<IllegalArgumentException> {
                assemblyService.assemble(
                    listOf(
                        TextFileArtifactContribution(sharedId, "left"),
                        TextFileArtifactContribution(sharedId, "right"),
                    ),
                )
            }
        }
    })
