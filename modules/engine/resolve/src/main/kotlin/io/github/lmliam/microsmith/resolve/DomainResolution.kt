package io.github.lmliam.microsmith.resolve

import arrow.core.NonEmptyList

sealed interface DomainResolution<out R : ResolvedModel> {
    data object NotApplicable : DomainResolution<Nothing>

    data class Success<R : ResolvedModel>(val model: R) : DomainResolution<R>

    data class Failure(val issues: NonEmptyList<ResolutionIssue>) : DomainResolution<Nothing>
}
