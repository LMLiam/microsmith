package io.github.lmliam.microsmith.artifact

import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.resolve.ResolvedModel

@InternalMicrosmithApi
public class ArtifactContributionService
internal constructor(private val contributorRegistry: ArtifactContributorRegistry) {
    public constructor(contributors: List<ArtifactContributor<*>>) : this(ArtifactContributorRegistry(contributors))

    public fun contribute(models: List<ResolvedModel>): List<ArtifactContribution<Artifact>> =
        models
            .sortedBy { it::class.qualifiedName ?: it::class.simpleName ?: it::class.toString() }
            .flatMap { model -> contributeForModel(model) }

    private fun <M : ResolvedModel> contributeForModel(model: M): List<ArtifactContribution<Artifact>> {
        val contributors = contributorRegistry.resolve(model)
        return contributors.flatMap { contributor -> contributor.contribute(model) }
    }
}
