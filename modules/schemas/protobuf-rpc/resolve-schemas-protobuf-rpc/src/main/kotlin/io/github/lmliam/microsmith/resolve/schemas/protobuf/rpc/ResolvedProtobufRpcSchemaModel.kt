package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNone
import io.github.lmliam.microsmith.resolve.schemas.ResolvedSchemasModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

@ConsistentCopyVisibility
data class ResolvedProtobufRpcSchemaModel private constructor(val schemas: List<ResolvedProtobufRpcSchema>) :
    ResolvedSchemasModel {
    sealed interface Rejection {
        data class DuplicateService(val identity: QualifiedSchemaName, val occurrences: Int) : Rejection
    }

    companion object {
        fun of(schemas: List<ResolvedProtobufRpcSchema>): EitherNel<Rejection, ResolvedProtobufRpcSchemaModel> {
            val ownSchemas = schemas.toList()
            val rejections =
                ownSchemas
                    .groupingBy { it.qualifiedName }
                    .eachCount()
                    .filterValues { it > 1 }
                    .map { (identity, occurrences) -> Rejection.DuplicateService(identity, occurrences) }

            return rejections
                .toNonEmptyListOrNone()
                .fold(
                    ifEmpty = { Either.Right(ResolvedProtobufRpcSchemaModel(ownSchemas)) },
                    ifSome = { Either.Left(it) },
                )
        }
    }
}
