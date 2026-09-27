package io.github.lmliam.microsmith.artifact

import io.github.lmliam.microsmith.resolve.ResolvedModel

class ArtifactContributionService(contributors: List<ArtifactContributor<*>>) {
    private val contributorRegistry = ArtifactContributorRegistry(contributors)

    fun contribute(models: List<ResolvedModel>): List<ArtifactContribution<out Artifact>> =
        models
            .sortedBy { it::class.qualifiedName ?: it::class.toString() }
            .flatMap { model ->
                contributorRegistry.resolve(model).flatMap { contributor -> contributor.contribute(model) }
            }
}
