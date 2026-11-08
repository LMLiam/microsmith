package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import arrow.core.raise.Raise
import arrow.core.raise.context.ensure
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufRejection

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
