package io.github.lmliam.microsmith.resolve

import arrow.core.NonEmptyList

sealed interface ResolutionOutcome {
    data class Success(val models: List<ResolvedModel>) : ResolutionOutcome

    data class Failure(val issues: NonEmptyList<ResolutionIssue>) : ResolutionOutcome
}
