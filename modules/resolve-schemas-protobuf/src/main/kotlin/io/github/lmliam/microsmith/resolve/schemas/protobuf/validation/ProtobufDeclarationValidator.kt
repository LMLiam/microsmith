package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal class ProtobufDeclarationValidator {
    fun validate(schemaName: String, declaration: Type): List<ProtobufResolutionIssue> = buildList {
        validateIdentifier(
            schemaName,
            ProtobufResolutionIssue.IdentifierLocation.Declaration,
            declaration.name,
        )

        when (declaration) {
            is Message ->
                addAll(
                    validateMessageDeclaration(
                        schemaName,
                        declaration,
                    ),
                )

            is Enum ->
                addAll(
                    validateEnumDeclaration(
                        schemaName,
                        declaration,
                    ),
                )
        }
    }
}
