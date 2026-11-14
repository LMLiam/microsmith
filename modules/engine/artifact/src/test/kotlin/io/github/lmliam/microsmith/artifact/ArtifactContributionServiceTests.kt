package io.github.lmliam.microsmith.artifact

import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.resolve.ResolvedModel
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import kotlin.reflect.KClass

private data class AlphaResolved(val name: String) : ResolvedModel

private data class BetaResolved(val name: String) : ResolvedModel

private class AlphaContributor : ArtifactContributor<AlphaResolved> {
    override val resolvedType = AlphaResolved::class

    override fun contribute(model: AlphaResolved): List<ArtifactContribution<Artifact>> =
        listOf(
            TextFileArtifactContribution(
                artifactId = TextFileArtifactId(Path.of("alpha.txt")),
                contents = model.name,
            ),
        )
}

private class BetaContributor : ArtifactContributor<BetaResolved> {
    override val resolvedType = BetaResolved::class

    override fun contribute(model: BetaResolved): List<ArtifactContribution<Artifact>> =
        listOf(
            TextFileArtifactContribution(
                artifactId = TextFileArtifactId(Path.of("beta.txt")),
                contents = model.name,
            ),
        )
}

private class MisdeclaredContributor : ArtifactContributor<AlphaResolved> {
    @Suppress("UNCHECKED_CAST") override val resolvedType = BetaResolved::class as KClass<AlphaResolved>

    override fun contribute(model: AlphaResolved): List<ArtifactContribution<Artifact>> = emptyList()
}

class ArtifactContributionServiceTests :
    StringSpec(
        {
            "routes models to matching contributors in model-type name order" {
                val service =
                    ArtifactContributionService(
                        listOf(
                            BetaContributor(),
                            AlphaContributor(),
                        ),
                    )

                val contributions = service.contribute(listOf(BetaResolved("second"), AlphaResolved("first")))

                contributions shouldContainExactly
                    listOf(
                        TextFileArtifactContribution(
                            artifactId = TextFileArtifactId(Path.of("alpha.txt")),
                            contents = "first",
                        ),
                        TextFileArtifactContribution(
                            artifactId = TextFileArtifactId(Path.of("beta.txt")),
                            contents = "second",
                        ),
                    )
            }

            "rejects contributor registrations whose declared resolvedType does not match generic type argument" {
                val error =
                    shouldThrow<IllegalArgumentException> {
                        ArtifactContributorRegistry(listOf(MisdeclaredContributor()))
                    }

                error.message shouldBe
                    "'io.github.lmliam.microsmith.artifact.MisdeclaredContributor' declares " +
                        "resolvedType 'io.github.lmliam.microsmith.artifact.BetaResolved', but implements " +
                        "ArtifactContributor<io.github.lmliam.microsmith.artifact.AlphaResolved>. " +
                        "Ensure resolvedType matches the ArtifactContributor generic type argument."
            }

            "rejects duplicate contributors registered for the same resolved type" {
                val error =
                    shouldThrow<IllegalArgumentException> {
                        ArtifactContributorRegistry(
                            listOf(
                                AlphaContributor(),
                                AlphaContributor(),
                            ),
                        )
                    }

                error.message shouldBe
                    "Duplicate artifact contributors registered for resolved type " +
                        "'io.github.lmliam.microsmith.artifact.AlphaResolved': " +
                        "io.github.lmliam.microsmith.artifact.AlphaContributor"
            }

            "returns empty contributions when no contributors match the model" {
                val service = ArtifactContributionService(listOf(AlphaContributor()))

                val contributions = service.contribute(listOf(BetaResolved("unhandled")))

                contributions.shouldBeEmpty()
            }
        },
    )
