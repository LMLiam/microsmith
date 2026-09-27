package io.github.lmliam.microsmith.resolve
import arrow.core.compareTo
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.dsl.extensions

class DomainResolutionService(resolvers: List<DomainResolver<*, *>>) {
    private val resolverRegistry = DomainResolverRegistry(resolvers)

    fun resolve(model: MicrosmithModel): ResolutionOutcome {
        val resolvedModels = mutableListOf<ResolvedModel>()
        val issues = mutableListOf<ResolutionIssue>()

        model.extensions()
            .sortedBy { it::class.qualifiedName ?: it::class.toString() }
            .forEach { extension ->
                resolverRegistry
                    .resolve(extension)
                    .forEach { resolver ->
                        when (val resolution = resolver.resolveUnchecked(extension)) {
                            DomainResolution.NotApplicable -> Unit
                            is DomainResolution.Success -> resolvedModels += resolution.model
                            is DomainResolution.Failure -> issues += resolution.issues
                        }
                    }
            }

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        return if (accumulatedIssues == null) {
            ResolutionOutcome.Success(models = resolvedModels.toList())
        } else {
            ResolutionOutcome.Failure(issues = accumulatedIssues)
        }
    }
}

private fun DomainResolver<MicrosmithExtension, ResolvedModel>.resolveUnchecked(
    extension: MicrosmithExtension,
): DomainResolution<ResolvedModel> = resolve(extension)
