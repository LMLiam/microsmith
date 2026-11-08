package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.ensure
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.raise
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufRejection

context(_: RaiseAccumulate<ResolvedProtobufRejection>)
internal fun rejectAll(rejections: List<ResolvedProtobufRejection>) {
    rejections.mapOrAccumulate { raise(it) }
}

context(_: Raise<ResolvedProtobufRejection>)
internal fun <T : Comparable<T>> checkNoDuplicates(values: List<T>, rejection: (List<T>) -> ResolvedProtobufRejection) {
    val duplicates = duplicateValues(values)

    ensure(duplicates.isEmpty()) { rejection(duplicates) }
}

private fun <T : Comparable<T>> duplicateValues(values: List<T>): List<T> =
    values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted()
