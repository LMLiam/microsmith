package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel

internal fun ResolvedProtobufSchemaModel.Rejection.toIssue(): ProtobufResolutionIssue =
    when (val rejection = this) {
        is ResolvedProtobufSchemaModel.Rejection.DuplicateSchema ->
            ProtobufResolutionIssue.SymbolNameCollision(
                scope = rejection.identity.packageName.orEmpty(),
                name = rejection.identity.typeName.value,
                declarations =
                    List(rejection.occurrences) {
                        ProtobufResolutionIssue.SymbolDeclaration.Type(rejection.identity.fullyQualifiedName)
                    },
            )

        is ResolvedProtobufSchemaModel.Rejection.CircularImports ->
            ProtobufResolutionIssue.CircularSchemaImports(rejection.schemas)

        is ResolvedProtobufSchemaModel.Rejection.UnresolvedReference ->
            ProtobufResolutionIssue.UnresolvedReference(
                rejection.schema.fullyQualifiedName,
                rejection.location,
                rejection.target.fullyQualifiedName,
            )

        // Unreachable from the resolver, which takes each reference's kind from the declared schema.
        is ResolvedProtobufSchemaModel.Rejection.ReferenceKindMismatch ->
            ProtobufResolutionIssue.UnresolvedReference(
                rejection.schema.fullyQualifiedName,
                rejection.location,
                rejection.target.fullyQualifiedName,
            )

        is ResolvedProtobufSchemaModel.Rejection.PackageSymbolCollision ->
            ProtobufResolutionIssue.SymbolNameCollision(rejection.packageName, rejection.name, rejection.declarations)
    }
