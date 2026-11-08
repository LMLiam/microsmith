package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNone
import io.github.lmliam.microsmith.resolve.ResolvedModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.SchemaNode
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.schemaGraphRejections

@ConsistentCopyVisibility
data class ResolvedProtobufSchemaModel private constructor(val schemas: List<ResolvedProtobufSchema>) : ResolvedModel {
    sealed interface Rejection {
        data class DuplicateSchema(val identity: QualifiedSchemaName, val occurrences: Int) : Rejection

        data class CircularImports(val schemas: List<String>) : Rejection
    }

    companion object {
        fun of(schemas: List<ResolvedProtobufSchema>): EitherNel<Rejection, ResolvedProtobufSchemaModel> {
            val ownSchemas = schemas.toList()
            return schemaGraphRejections(ownSchemas.map { SchemaNode(it.identity, it.dependencies) })
                .toNonEmptyListOrNone()
                .fold(ifEmpty = { Either.Right(ResolvedProtobufSchemaModel(ownSchemas)) }, ifSome = { Either.Left(it) })
        }
    }
}
