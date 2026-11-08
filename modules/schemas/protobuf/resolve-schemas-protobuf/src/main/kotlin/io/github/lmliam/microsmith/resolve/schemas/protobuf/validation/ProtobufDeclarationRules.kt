package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.ensure
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.raise
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufRejection
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan

context(_: Raise<ResolvedProtobufRejection>)
internal fun checkUniqueFieldNames(names: List<String>) =
    checkNoDuplicates(names, ResolvedProtobufRejection::DuplicateFieldNames)

context(_: Raise<ResolvedProtobufRejection>)
internal fun checkUniqueFieldNumbers(numbers: List<Int>) =
    checkNoDuplicates(numbers, ResolvedProtobufRejection::DuplicateFieldNumbers)

context(_: Raise<ResolvedProtobufRejection>)
internal fun checkUniqueOneofNames(names: List<String>) =
    checkNoDuplicates(names, ResolvedProtobufRejection::DuplicateOneofNames)

context(_: Raise<ResolvedProtobufRejection>)
internal fun checkEnumStartsAtZero(firstValueName: String, firstValueNumber: Int) {
    ensure(firstValueNumber == 0) {
        ResolvedProtobufRejection.EnumFirstValueMustBeZero(firstValueName, firstValueNumber)
    }
}

context(_: Raise<ResolvedProtobufRejection>)
internal fun checkUniqueEnumValueNames(names: List<String>) =
    checkNoDuplicates(names, ResolvedProtobufRejection::DuplicateEnumValueNames)

context(_: Raise<ResolvedProtobufRejection>)
internal fun checkUniqueEnumValueNumbers(numbers: List<Int>) =
    checkNoDuplicates(numbers, ResolvedProtobufRejection::DuplicateEnumValueNumbers)

context(_: RaiseAccumulate<ResolvedProtobufRejection>)
internal fun checkSpansReachFieldNumbers(spans: List<ReservedSpan>) {
    val fieldNumbers = FieldNumber.MIN_VALUE..FieldNumber.MAX_VALUE
    val unreachable = spans.flatMap { span ->
        listOf(span.first, span.last).distinct().filter { it !in fieldNumbers }.map { number -> span to number }
    }

    unreachable.mapOrAccumulate { (span, number) ->
        raise(ResolvedProtobufRejection.ReservedNumberOutOfRange(span.notation, number))
    }
}
