package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.resolve.schemas.protobuf.LocatedReference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchema
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.kind
import io.github.lmliam.microsmith.resolve.schemas.protobuf.references
import io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution.ProtobufDeclarationKind

internal fun findReferenceRejections(
    schemas: List<ResolvedProtobufSchema>
): List<ResolvedProtobufSchemaModel.Rejection> {
    val declaredKinds =
        schemas.groupBy({ it.identity.fullyQualifiedName }, { it.declaration.kind() }).mapValues { it.value.toSet() }

    return schemas.flatMap { schema ->
        schema.declaration.references().mapNotNull { reference ->
            referenceRejection(schema, reference, declaredKinds[reference.reference.target.fullyQualifiedName])
        }
    }
}

private fun referenceRejection(
    schema: ResolvedProtobufSchema,
    located: LocatedReference,
    declaredKinds: Set<ProtobufDeclarationKind>?,
): ResolvedProtobufSchemaModel.Rejection? {
    val target = located.reference.target
    val referencedAs = located.reference.kind

    return when {
        declaredKinds == null ->
            ResolvedProtobufSchemaModel.Rejection.UnresolvedReference(schema.identity, located.location, target)

        referencedAs !in declaredKinds ->
            ResolvedProtobufSchemaModel.Rejection.ReferenceKindMismatch(
                schema.identity,
                located.location,
                target,
                referencedAs,
                declaredKinds.first(),
            )

        else -> null
    }
}
