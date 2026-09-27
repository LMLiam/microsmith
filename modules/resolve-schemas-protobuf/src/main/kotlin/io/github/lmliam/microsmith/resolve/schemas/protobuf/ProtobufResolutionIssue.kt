package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.ResolutionIssue

sealed interface ProtobufResolutionIssue : ResolutionIssue {
    sealed interface ReferenceIssue : ProtobufResolutionIssue

    sealed interface DeclarationIssue : ProtobufResolutionIssue

    sealed interface EnumIssue : ProtobufResolutionIssue

    sealed interface ReservationIssue : ProtobufResolutionIssue

    data class UnresolvedReference(val schemaName: String, val location: ReferenceLocation, val targetName: String) :
        ReferenceIssue

    data class SchemaDeclarationNameMismatch(val schemaName: String, val declarationName: String) : DeclarationIssue

    data class InvalidIdentifier(val schemaName: String, val location: IdentifierLocation, val value: String) :
        DeclarationIssue

    data class InvalidFieldNumber(val schemaName: String, val location: FieldLocation, val number: Int) :
        DeclarationIssue

    data class DuplicateFieldNames(val schemaName: String, val names: List<String>) : DeclarationIssue

    data class DuplicateFieldNumbers(val schemaName: String, val numbers: List<Int>) : DeclarationIssue

    data class DuplicateOneofNames(val schemaName: String, val names: List<String>) : DeclarationIssue

    data class EmptyOneof(val schemaName: String, val oneofName: String) : DeclarationIssue

    data class EmptyEnum(val schemaName: String) : EnumIssue

    data class EnumFirstValueMustBeZero(val schemaName: String, val firstValueName: String, val number: Int) :
        EnumIssue

    data class DuplicateEnumValueNames(val schemaName: String, val names: List<String>) : EnumIssue

    data class DuplicateEnumValueNumbers(val schemaName: String, val numbers: List<Int>) : EnumIssue

    data class InvalidReservationRange(val schemaName: String, val start: Int, val endInclusive: Int) :
        ReservationIssue

    data class DuplicateReservedNames(val schemaName: String, val names: List<String>) : ReservationIssue

    data class ReservedNameCollision(val schemaName: String, val names: List<String>) : ReservationIssue

    data class OverlappingReservedNumbers(val schemaName: String) : ReservationIssue

    data class ReservedNumberCollision(val schemaName: String, val numbers: List<Int>) : ReservationIssue

    sealed interface IdentifierLocation {
        data object Declaration : IdentifierLocation

        data class Field(val fieldName: String) : IdentifierLocation

        data class Oneof(val oneofName: String) : IdentifierLocation

        data class OneofField(val oneofName: String, val fieldName: String) : IdentifierLocation

        data class EnumValue(val valueName: String) : IdentifierLocation

        data class Reservation(val name: String) : IdentifierLocation
    }

    sealed interface FieldLocation {
        data class Field(val fieldName: String) : FieldLocation

        data class OneofField(val oneofName: String, val fieldName: String) : FieldLocation

        data class Reservation(val description: String) : FieldLocation
    }

    sealed interface ReferenceLocation {
        data class Field(val fieldName: String) : ReferenceLocation

        data class MapValue(val fieldName: String) : ReferenceLocation

        data class OneofField(val oneofName: String, val fieldName: String) : ReferenceLocation
    }
}
