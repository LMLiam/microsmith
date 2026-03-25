package io.github.lmliam.microsmith.lower.core

import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembly
import io.github.lmliam.microsmith.artifact.core.ArtifactAssemblyService

class ArtifactLoweringService internal constructor(
    private val lowererRegistry: ArtifactLowererRegistry = ArtifactLowererRegistry(),
    private val assemblyService: ArtifactAssemblyService = ArtifactAssemblyService(),
) {
    constructor() : this(ArtifactLowererRegistry(), ArtifactAssemblyService())

    constructor(
        lowerers: List<ArtifactLowerer<*>>,
        assemblyService: ArtifactAssemblyService = ArtifactAssemblyService(),
    ) : this(ArtifactLowererRegistry(lowerers), assemblyService)

    fun lower(assembly: ArtifactAssembly): ArtifactAssembly {
        var current = assembly
        val seenSignatures = linkedSetOf<String>()

        while (true) {
            val signature = current.signature()
            require(seenSignatures.add(signature)) {
                "Artifact lowering cycle detected for assembly: $signature"
            }

            val passthroughArtifacts = mutableListOf<Artifact>()
            val loweredContributions =
                mutableListOf<io.github.lmliam.microsmith.artifact.core.ArtifactContribution<out Artifact>>()
            var loweredAny = false

            current.artifacts().forEach { artifact ->
                val lowerer = lowererRegistry.resolveOrNull(artifact)
                if (lowerer == null) {
                    passthroughArtifacts += artifact
                    return@forEach
                }

                val contributions = lowerer.lowerUnchecked(artifact)
                require(contributions.none { it.artifactId.artifactType == artifact.id.artifactType }) {
                    val lowererName = lowerer::class.qualifiedName ?: lowerer::class.toString()
                    val artifactTypeName = artifact.id.artifactType.toString()
                    "Artifact lowerer $lowererName lowered $artifactTypeName into the same artifact type, " +
                        "which would create an immediate lowering cycle."
                }
                loweredContributions += contributions
                loweredAny = true
            }

            if (!loweredAny) {
                return current
            }

            current = assemblyService.assembleRetaining(passthroughArtifacts, loweredContributions)
        }
    }

    private fun ArtifactAssembly.signature(): String = artifacts()
        .map { artifact ->
            val typeName = artifact.id.artifactType.qualifiedName ?: artifact.id.artifactType.toString()
            "$typeName:${artifact.id}"
        }
        .sorted()
        .joinToString("|")
}

@Suppress("UNCHECKED_CAST")
private fun ArtifactLowerer<Artifact>.lowerUnchecked(
    artifact: Artifact,
): List<io.github.lmliam.microsmith.artifact.core.ArtifactContribution<out Artifact>> =
    (this as ArtifactLowerer<Artifact>).lower(artifact)
