package io.github.lmliam.microsmith.resolve

import arrow.core.NonEmptyList
import io.github.lmliam.microsmith.InternalMicrosmithApi

@InternalMicrosmithApi
public sealed interface ResolutionOutcome {
    @InternalMicrosmithApi public data class Success(public val models: List<ResolvedModel>) : ResolutionOutcome

    @InternalMicrosmithApi
    public data class Failure(public val issues: NonEmptyList<ResolutionIssue>) : ResolutionOutcome
}
