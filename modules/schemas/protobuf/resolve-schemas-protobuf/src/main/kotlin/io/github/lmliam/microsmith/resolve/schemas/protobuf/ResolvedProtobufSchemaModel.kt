package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNone
import io.github.lmliam.microsmith.resolve.schemas.ResolvedSchemasModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution.ProtobufDeclarationKind
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.SchemaNode
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.findPackageSymbolCollisions
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.findReferenceRejections
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.schemaGraphRejections

@ConsistentCopyVisibility
public data class ResolvedProtobufSchemaModel private constructor(public val schemas: List<ResolvedProtobufSchema>) :
    ResolvedSchemasModel {
    public sealed interface Rejection {
        public data class DuplicateSchema(public val identity: QualifiedSchemaName, public val occurrences: Int) :
            Rejection

        public data class CircularImports(public val schemas: List<String>) : Rejection

        public data class UnresolvedReference(
            public val schema: QualifiedSchemaName,
            public val location: ProtobufResolutionIssue.ReferenceLocation,
            public val target: QualifiedSchemaName,
        ) : Rejection

        public data class ReferenceKindMismatch(
            public val schema: QualifiedSchemaName,
            public val location: ProtobufResolutionIssue.ReferenceLocation,
            public val target: QualifiedSchemaName,
            public val referencedAs: ProtobufDeclarationKind,
            public val declaredAs: ProtobufDeclarationKind,
        ) : Rejection

        public data class PackageSymbolCollision(
            public val packageName: String,
            public val name: String,
            public val declarations: List<ProtobufResolutionIssue.SymbolDeclaration>,
        ) : Rejection
    }

    public companion object {
        public fun of(schemas: List<ResolvedProtobufSchema>): EitherNel<Rejection, ResolvedProtobufSchemaModel> {
            val ownSchemas = schemas.toList()
            val rejections =
                findReferenceRejections(ownSchemas) +
                    findPackageSymbolCollisions(ownSchemas) +
                    schemaGraphRejections(ownSchemas.map { SchemaNode(it.identity, it.dependencies) })

            return rejections
                .toNonEmptyListOrNone()
                .fold(ifEmpty = { Either.Right(ResolvedProtobufSchemaModel(ownSchemas)) }, ifSome = { Either.Left(it) })
        }
    }
}
