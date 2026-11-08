package io.github.lmliam.microsmith.artifact

import io.github.lmliam.microsmith.resolve.ResolvedModel

class ArtifactContributionService internal constructor(private val contributorRegistry: ArtifactContributorRegistry) {
    constructor(contributors: List<ArtifactContributor<*>>) : this(ArtifactContributorRegistry(contributors))

    fun contribute(models: List<ResolvedModel>): List<ArtifactContribution<Artifact>> =
        models
            .sortedBy { it::class.qualifiedName ?: it::class.simpleName ?: it::class.toString() }
            .flatMap { model -> contributeForModel(model) }

    private fun <M : ResolvedModel> contributeForModel(model: M): List<ArtifactContribution<Artifact>> {
        val contributors = contributorRegistry.resolve(model)
        return contributors.flatMap { contributor -> contributor.contribute(model) }
    }
}
