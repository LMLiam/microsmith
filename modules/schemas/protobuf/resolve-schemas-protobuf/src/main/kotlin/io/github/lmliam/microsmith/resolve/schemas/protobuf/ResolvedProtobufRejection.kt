package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan

public sealed interface ResolvedProtobufRejection {
    public data class DuplicateFieldNames(public val names: List<String>) : ResolvedProtobufRejection

    public data class DuplicateFieldNumbers(public val numbers: List<Int>) : ResolvedProtobufRejection

    public data class DuplicateOneofNames(public val names: List<String>) : ResolvedProtobufRejection

    public data class FieldJsonNameCollision(public val jsonName: String, public val fieldNames: List<String>) :
        ResolvedProtobufRejection

    public data class MessageSymbolCollision(public val name: String, public val declarations: List<MessageSymbol>) :
        ResolvedProtobufRejection

    public data class EnumFirstValueMustBeZero(public val firstValueName: String, public val number: Int) :
        ResolvedProtobufRejection

    public data class DuplicateEnumValueNames(public val names: List<String>) : ResolvedProtobufRejection

    public data class DuplicateEnumValueNumbers(public val numbers: List<Int>) : ResolvedProtobufRejection

    public data class EnumValueNameCollision(public val valueNames: List<String>) : ResolvedProtobufRejection

    public data class DuplicateReservedNames(public val names: List<String>) : ResolvedProtobufRejection

    public data object OverlappingReservedNumbers : ResolvedProtobufRejection

    public data class ReservedNameCollision(public val names: List<String>) : ResolvedProtobufRejection

    public data class ReservedNumberCollision(public val numbers: List<Int>) : ResolvedProtobufRejection

    public data class ReservedSpanDomainMismatch(
        public val notation: String,
        public val first: Int,
        public val expected: ReservedSpan.Domain,
    ) : ResolvedProtobufRejection

    public sealed interface MessageSymbol {
        public data class Field(public val fieldName: String) : MessageSymbol

        public data class Oneof(public val oneofName: String) : MessageSymbol

        public data class MapEntry(public val fieldName: String) : MessageSymbol
    }
}
