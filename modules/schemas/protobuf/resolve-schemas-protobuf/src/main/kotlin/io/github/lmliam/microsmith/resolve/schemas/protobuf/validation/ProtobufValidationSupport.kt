package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import arrow.core.raise.Raise
import arrow.core.raise.context.ensure
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

context(_: Raise<ProtobufResolutionIssue>)
internal fun <T : Comparable<T>> checkNoDuplicates(values: List<T>, issue: (List<T>) -> ProtobufResolutionIssue) {
    val duplicates = duplicateValues(values)

    ensure(duplicates.isEmpty()) { issue(duplicates) }
}

private fun <T : Comparable<T>> duplicateValues(values: List<T>): List<T> =
    values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted()
