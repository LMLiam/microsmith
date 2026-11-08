package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNone
import io.github.lmliam.microsmith.resolve.ResolvedModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution.ProtobufDeclarationKind
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.SchemaNode
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.findPackageSymbolCollisions
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.findReferenceRejections
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.schemaGraphRejections

@ConsistentCopyVisibility
data class ResolvedProtobufSchemaModel private constructor(val schemas: List<ResolvedProtobufSchema>) : ResolvedModel {
    sealed interface Rejection {
        data class DuplicateSchema(val identity: QualifiedSchemaName, val occurrences: Int) : Rejection

        data class CircularImports(val schemas: List<String>) : Rejection

        data class UnresolvedReference(
            val schema: QualifiedSchemaName,
            val location: ProtobufResolutionIssue.ReferenceLocation,
            val target: QualifiedSchemaName,
        ) : Rejection

        data class ReferenceKindMismatch(
            val schema: QualifiedSchemaName,
            val location: ProtobufResolutionIssue.ReferenceLocation,
            val target: QualifiedSchemaName,
            val referencedAs: ProtobufDeclarationKind,
            val declaredAs: ProtobufDeclarationKind,
        ) : Rejection

        data class PackageSymbolCollision(
            val packageName: String,
            val name: String,
            val declarations: List<ProtobufResolutionIssue.SymbolDeclaration>,
        ) : Rejection
    }

    companion object {
        fun of(schemas: List<ResolvedProtobufSchema>): EitherNel<Rejection, ResolvedProtobufSchemaModel> {
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
