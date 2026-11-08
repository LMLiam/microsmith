package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan

sealed interface ResolvedProtobufRejection {
    data class DuplicateFieldNames(val names: List<String>) : ResolvedProtobufRejection

    data class DuplicateFieldNumbers(val numbers: List<Int>) : ResolvedProtobufRejection

    data class DuplicateOneofNames(val names: List<String>) : ResolvedProtobufRejection

    data class FieldJsonNameCollision(val jsonName: String, val fieldNames: List<String>) : ResolvedProtobufRejection

    data class MessageSymbolCollision(val name: String, val declarations: List<MessageSymbol>) :
        ResolvedProtobufRejection

    data class EnumFirstValueMustBeZero(val firstValueName: String, val number: Int) : ResolvedProtobufRejection

    data class DuplicateEnumValueNames(val names: List<String>) : ResolvedProtobufRejection

    data class DuplicateEnumValueNumbers(val numbers: List<Int>) : ResolvedProtobufRejection

    data class EnumValueNameCollision(val valueNames: List<String>) : ResolvedProtobufRejection

    data class DuplicateReservedNames(val names: List<String>) : ResolvedProtobufRejection

    data object OverlappingReservedNumbers : ResolvedProtobufRejection

    data class ReservedNameCollision(val names: List<String>) : ResolvedProtobufRejection

    data class ReservedNumberCollision(val numbers: List<Int>) : ResolvedProtobufRejection

    data class ReservedSpanDomainMismatch(val notation: String, val first: Int, val expected: ReservedSpan.Domain) :
        ResolvedProtobufRejection

    sealed interface MessageSymbol {
        data class Field(val fieldName: String) : MessageSymbol

        data class Oneof(val oneofName: String) : MessageSymbol

        data class MapEntry(val fieldName: String) : MessageSymbol
    }
}
