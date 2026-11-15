package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.ResolutionIssue

public sealed interface ProtobufResolutionIssue : ResolutionIssue {
    public sealed interface ReferenceIssue : ProtobufResolutionIssue

    public sealed interface DeclarationIssue : ProtobufResolutionIssue

    public sealed interface EnumIssue : ProtobufResolutionIssue

    public sealed interface ReservationIssue : ProtobufResolutionIssue

    public data class UnresolvedReference(
        public val schemaName: String,
        public val location: ReferenceLocation,
        public val targetName: String,
    ) : ReferenceIssue

    public data class CircularSchemaImports(public val schemas: List<String>) : ReferenceIssue

    public data class InvalidReferenceName(
        public val schemaName: String,
        public val location: ReferenceLocation,
        public val targetName: String,
        public val reason: InvalidSchemaName.Reason,
    ) : ReferenceIssue

    public data class SchemaDeclarationNameMismatch(public val schemaName: String, public val declarationName: String) :
        DeclarationIssue

    public data class InvalidSchemaName(public val schemaName: String, public val reason: Reason) : DeclarationIssue {
        public enum class Reason {
            BLANK,
            LEADING_DOT,
            TRAILING_DOT,
            WHITESPACE,
            EMPTY_SEGMENT,
            INVALID_IDENTIFIER_SEGMENT,
        }
    }

    public data class InvalidIdentifier(
        public val schemaName: String,
        public val location: IdentifierLocation,
        public val value: String,
    ) : DeclarationIssue

    public data class InvalidFieldNumber(
        public val schemaName: String,
        public val location: FieldLocation,
        public val number: Int,
    ) : DeclarationIssue

    public data class DuplicateFieldNames(public val schemaName: String, public val names: List<String>) :
        DeclarationIssue

    public data class DuplicateFieldNumbers(public val schemaName: String, public val numbers: List<Int>) :
        DeclarationIssue

    public data class DuplicateOneofNames(public val schemaName: String, public val names: List<String>) :
        DeclarationIssue

    public data class EmptyOneof(public val schemaName: String, public val oneofName: String) : DeclarationIssue

    public data class SymbolNameCollision(
        public val scope: String,
        public val name: String,
        public val declarations: List<SymbolDeclaration>,
    ) : DeclarationIssue

    public data class FieldJsonNameCollision(
        public val schemaName: String,
        public val jsonName: String,
        public val fieldNames: List<String>,
    ) : DeclarationIssue

    public data class EmptyEnum(public val schemaName: String) : EnumIssue

    public data class EnumFirstValueMustBeZero(
        public val schemaName: String,
        public val firstValueName: String,
        public val number: Int,
    ) : EnumIssue

    public data class DuplicateEnumValueNames(public val schemaName: String, public val names: List<String>) : EnumIssue

    public data class DuplicateEnumValueNumbers(public val schemaName: String, public val numbers: List<Int>) :
        EnumIssue

    public data class EnumValueNameCollision(public val schemaName: String, public val valueNames: List<String>) :
        EnumIssue

    public data class InvalidReservationRange(
        public val schemaName: String,
        public val start: Int,
        public val endInclusive: Int,
    ) : ReservationIssue

    public data class DuplicateReservedNames(public val schemaName: String, public val names: List<String>) :
        ReservationIssue

    public data class ReservedNameCollision(public val schemaName: String, public val names: List<String>) :
        ReservationIssue

    public data class OverlappingReservedNumbers(public val schemaName: String) : ReservationIssue

    public data class ReservedNumberCollision(public val schemaName: String, public val numbers: List<Int>) :
        ReservationIssue

    public sealed interface IdentifierLocation {
        public data object Declaration : IdentifierLocation

        public data class Field(public val fieldName: String) : IdentifierLocation

        public data class Oneof(public val oneofName: String) : IdentifierLocation

        public data class OneofField(public val oneofName: String, public val fieldName: String) : IdentifierLocation

        public data class EnumValue(public val valueName: String) : IdentifierLocation

        public data class Reservation(public val name: String) : IdentifierLocation
    }

    public sealed interface SymbolDeclaration {
        public data class Package(public val packageName: String) : SymbolDeclaration

        public data class Type(public val schemaName: String) : SymbolDeclaration

        public data class EnumValue(public val schemaName: String, public val valueName: String) : SymbolDeclaration

        public data class Field(public val schemaName: String, public val fieldName: String) : SymbolDeclaration

        public data class Oneof(public val schemaName: String, public val oneofName: String) : SymbolDeclaration

        public data class MapEntry(public val schemaName: String, public val fieldName: String) : SymbolDeclaration
    }

    public sealed interface FieldLocation {
        public data class Field(public val fieldName: String) : FieldLocation

        public data class OneofField(public val oneofName: String, public val fieldName: String) : FieldLocation

        public data class Reservation(public val description: String) : FieldLocation
    }

    public sealed interface ReferenceLocation {
        public data class Field(public val fieldName: String) : ReferenceLocation

        public data class MapValue(public val fieldName: String) : ReferenceLocation

        public data class OneofField(public val oneofName: String, public val fieldName: String) : ReferenceLocation
    }
}
