package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.ensure
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.raise
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufRejection
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
internal fun checkSpansBelongTo(domain: ReservedSpan.Domain, spans: List<ReservedSpan>) {
    spans
        .filter { it.domain != domain }
        .mapOrAccumulate { span ->
            raise(ResolvedProtobufRejection.ReservedSpanDomainMismatch(span.notation, span.first, domain))
        }
}
