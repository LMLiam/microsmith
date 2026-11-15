package io.github.lmliam.microsmith.resolve

import arrow.core.NonEmptyList

public sealed interface DomainResolution<out R : ResolvedModel> {
    public data object NotApplicable : DomainResolution<Nothing>

    public data class Success<R : ResolvedModel>(public val model: R) : DomainResolution<R>

    public data class Failure(public val issues: NonEmptyList<ResolutionIssue>) : DomainResolution<Nothing>
}
