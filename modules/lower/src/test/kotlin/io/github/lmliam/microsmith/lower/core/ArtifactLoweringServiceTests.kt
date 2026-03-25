package io.github.lmliam.microsmith.lower.core

import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactAssemblyService
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactId
import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactAssembler
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import java.nio.file.Path
import kotlin.reflect.KClass

class ArtifactLoweringServiceTests :
    StringSpec({
        "lower recursively lowers artifacts until only terminal artifacts remain" {
            val assemblyService = ArtifactAssemblyService(
                listOf(
                    StageOneArtifactAssembler(),
                    StageTwoArtifactAssembler(),
                    TextFileArtifactAssembler(),
                ),
            )
            val loweringService = ArtifactLoweringService(
                lowerers = listOf(StageOneLowerer(), StageTwoLowerer()),
                assemblyService = assemblyService,
            )
            val initial = assemblyService.assemble(listOf(StageOneContribution(StageOneId("start"), "start")))

            val lowered = loweringService.lower(initial)

            lowered.artifacts().map { it.id.artifactType } shouldContainExactly listOf(TextFileArtifact::class)
        }

        "lower rejects immediate self-cycles from lowerers" {
            val assemblyService = ArtifactAssemblyService(listOf(StageOneArtifactAssembler()))
            val loweringService = ArtifactLoweringService(
                lowerers = listOf(SelfCyclingLowerer()),
                assemblyService = assemblyService,
            )
            val initial = assemblyService.assemble(listOf(StageOneContribution(StageOneId("start"), "start")))

            shouldThrow<IllegalArgumentException> {
                loweringService.lower(initial)
            }
        }
    })

private data class StageOneArtifact(
    override val id: StageOneId,
    val contents: String,
) : Artifact

private data class StageOneId(
    val name: String,
) : ArtifactId<StageOneArtifact> {
    override val artifactType: KClass<StageOneArtifact> = StageOneArtifact::class
}

private data class StageOneContribution(
    override val artifactId: StageOneId,
    val contents: String,
) : ArtifactContribution<StageOneArtifact>

private class StageOneArtifactAssembler : ArtifactAssembler<StageOneArtifact> {
    override val artifactType: KClass<StageOneArtifact> = StageOneArtifact::class

    override fun create(first: ArtifactContribution<StageOneArtifact>): StageOneArtifact =
        StageOneArtifact(requireContribution(first).artifactId, requireContribution(first).contents)

    override fun merge(
        current: StageOneArtifact,
        contribution: ArtifactContribution<StageOneArtifact>,
    ): StageOneArtifact {
        val next = requireContribution(contribution)
        require(current.contents == next.contents)
        return current
    }

    private fun requireContribution(contribution: ArtifactContribution<StageOneArtifact>): StageOneContribution {
        require(contribution is StageOneContribution)
        return contribution
    }
}

private data class StageTwoArtifact(
    override val id: StageTwoId,
    val contents: String,
) : Artifact

private data class StageTwoId(
    val name: String,
) : ArtifactId<StageTwoArtifact> {
    override val artifactType: KClass<StageTwoArtifact> = StageTwoArtifact::class
}

private data class StageTwoContribution(
    override val artifactId: StageTwoId,
    val contents: String,
) : ArtifactContribution<StageTwoArtifact>

private class StageTwoArtifactAssembler : ArtifactAssembler<StageTwoArtifact> {
    override val artifactType: KClass<StageTwoArtifact> = StageTwoArtifact::class

    override fun create(first: ArtifactContribution<StageTwoArtifact>): StageTwoArtifact =
        StageTwoArtifact(requireContribution(first).artifactId, requireContribution(first).contents)

    override fun merge(
        current: StageTwoArtifact,
        contribution: ArtifactContribution<StageTwoArtifact>,
    ): StageTwoArtifact {
        val next = requireContribution(contribution)
        require(current.contents == next.contents)
        return current
    }

    private fun requireContribution(contribution: ArtifactContribution<StageTwoArtifact>): StageTwoContribution {
        require(contribution is StageTwoContribution)
        return contribution
    }
}

private class StageOneLowerer : ArtifactLowerer<StageOneArtifact> {
    override val artifactType: KClass<StageOneArtifact> = StageOneArtifact::class

    override fun lower(artifact: StageOneArtifact): List<ArtifactContribution<out Artifact>> {
        return listOf(StageTwoContribution(StageTwoId("middle"), artifact.contents))
    }
}

private class StageTwoLowerer : ArtifactLowerer<StageTwoArtifact> {
    override val artifactType: KClass<StageTwoArtifact> = StageTwoArtifact::class

    override fun lower(artifact: StageTwoArtifact): List<ArtifactContribution<out Artifact>> {
        return listOf(TextFileArtifactContribution(TextFileArtifactId(Path.of("result.txt")), artifact.contents))
    }
}

private class SelfCyclingLowerer : ArtifactLowerer<StageOneArtifact> {
    override val artifactType: KClass<StageOneArtifact> = StageOneArtifact::class

    override fun lower(artifact: StageOneArtifact): List<ArtifactContribution<out Artifact>> {
        return listOf(StageOneContribution(StageOneId("cycle"), artifact.contents))
    }
}
