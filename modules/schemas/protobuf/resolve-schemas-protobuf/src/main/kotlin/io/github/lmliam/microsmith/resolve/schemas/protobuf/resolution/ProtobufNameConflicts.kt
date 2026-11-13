package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.ServiceDeclaration
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing.toIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.findEnumValueNameCollisions
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.findJsonNameCollisions

internal fun findNameConflicts(schemas: List<ProtobufSchema>): List<ProtobufResolutionIssue> =
    schemas.sortedBy(ProtobufSchema::name).flatMap(::nameConflictsOf)

private fun nameConflictsOf(schema: ProtobufSchema): List<ProtobufResolutionIssue> =
    QualifiedSchemaName.parse(schema.name)
        .fold(
            ifLeft = { emptyList() },
            ifRight = { identity ->
                when (val declaration = schema.schema) {
                    is Message -> fieldJsonNameCollisions(identity.fullyQualifiedName, declaration)

                    is Enum -> enumValueNameCollisions(identity, declaration)

                    is ServiceDeclaration -> emptyList()
                }
            },
        )

private fun fieldJsonNameCollisions(schemaName: String, message: Message): List<ProtobufResolutionIssue> =
    findJsonNameCollisions(
            (message.fields + message.oneofs.flatMap(Oneof::fields)).map(Field::name).filter(::isIdentifier)
        )
        .map { it.toIssue(schemaName) }

private fun enumValueNameCollisions(identity: QualifiedSchemaName, enum: Enum): List<ProtobufResolutionIssue> =
    findEnumValueNameCollisions(identity.typeName.value, enum.values.map { it.name }.filter(::isIdentifier)).map {
        it.toIssue(identity.fullyQualifiedName)
    }
