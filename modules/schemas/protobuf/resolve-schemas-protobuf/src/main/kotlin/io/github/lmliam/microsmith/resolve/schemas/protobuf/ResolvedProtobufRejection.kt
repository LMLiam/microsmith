package io.github.lmliam.microsmith.resolve.schemas.protobuf

sealed interface ResolvedProtobufRejection {
    data class DuplicateFieldNames(val names: List<String>) : ResolvedProtobufRejection

    data class DuplicateFieldNumbers(val numbers: List<Int>) : ResolvedProtobufRejection

    data class DuplicateOneofNames(val names: List<String>) : ResolvedProtobufRejection

    data class EnumFirstValueMustBeZero(val firstValueName: String, val number: Int) : ResolvedProtobufRejection

    data class DuplicateEnumValueNames(val names: List<String>) : ResolvedProtobufRejection

    data class DuplicateEnumValueNumbers(val numbers: List<Int>) : ResolvedProtobufRejection

    data class DuplicateReservedNames(val names: List<String>) : ResolvedProtobufRejection

    data object OverlappingReservedNumbers : ResolvedProtobufRejection

    data class ReservedNameCollision(val names: List<String>) : ResolvedProtobufRejection

    data class ReservedNumberCollision(val numbers: List<Int>) : ResolvedProtobufRejection

    data class ReservedNumberOutOfRange(val notation: String, val number: Int) : ResolvedProtobufRejection
}
