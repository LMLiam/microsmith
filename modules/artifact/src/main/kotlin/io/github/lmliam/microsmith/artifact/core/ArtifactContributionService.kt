package io.github.lmliam.microsmith.artifact.core

import io.github.lmliam.microsmith.resolve.core.ResolvedModel

class ArtifactContributionService {
    private val contributorRegistry: ArtifactContributorRegistry

    constructor() {
        contributorRegistry = ArtifactContributorRegistry()
    }

    internal constructor(contributorRegistry: ArtifactContributorRegistry) {
        this.contributorRegistry = contributorRegistry
    }

    fun contribute(models: List<ResolvedModel>): List<ArtifactContribution<out Artifact>> = models
        .sortedBy { it::class.qualifiedName ?: it::class.toString() }
        .flatMap { model ->
            contributorRegistry.resolve(model).flatMap { contributor ->
                contributor.contributeUnchecked(model)
            }
        }
}

@Suppress("UNCHECKED_CAST")
private fun ArtifactContributor<ResolvedModel>.contributeUnchecked(
    model: ResolvedModel,
): List<ArtifactContribution<out Artifact>> = (this as ArtifactContributor<ResolvedModel>).contribute(model)
