package io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun ProtobufResolutionIssue.EnumIssue.toDiagnostic(): ResolutionDiagnostic = when (this) {
    is ProtobufResolutionIssue.EmptyEnum ->
        protobufDiagnostic(
            code = "protobuf.empty-enum",
            message =
            "Protobuf enum '$schemaName' has no values.",
        )

    is ProtobufResolutionIssue.EnumFirstValueMustBeZero ->
        protobufDiagnostic(
            code = "protobuf.enum-first-value-not-zero",
            message =
            "Protobuf enum '$schemaName' first value '$firstValueName' " +
                "uses number $number; the first value must use zero.",
        )

    is ProtobufResolutionIssue.DuplicateEnumValueNames ->
        protobufDiagnostic(
            code = "protobuf.duplicate-enum-value-names",
            message =
            "Protobuf enum '$schemaName' has duplicate value names: " +
                names.joinToString(),
        )

    is ProtobufResolutionIssue.DuplicateEnumValueNumbers ->
        protobufDiagnostic(
            code = "protobuf.duplicate-enum-value-numbers",
            message =
            "Protobuf enum '$schemaName' has duplicate value numbers: " +
                numbers.joinToString(),
        )
}
