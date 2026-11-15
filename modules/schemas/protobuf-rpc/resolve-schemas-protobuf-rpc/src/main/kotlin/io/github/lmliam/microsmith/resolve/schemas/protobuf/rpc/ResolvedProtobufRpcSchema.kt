package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNone
import io.github.lmliam.microsmith.resolve.schemas.protobuf.dependenciesOf
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

@ConsistentCopyVisibility
public data class ResolvedProtobufRpcSchema
private constructor(val qualifiedName: QualifiedSchemaName, val rpcs: List<ResolvedProtobufRpc>) {
    public val dependencies: List<QualifiedSchemaName> =
        dependenciesOf(qualifiedName, rpcs.flatMap { rpc -> listOf(rpc.request.target, rpc.response.target) })

    public sealed interface Rejection {
        public data class DuplicateRpcName(public val name: ProtoIdentifier) : Rejection
    }

    public companion object {
        public fun of(
            qualifiedName: QualifiedSchemaName,
            rpcs: List<ResolvedProtobufRpc>,
        ): EitherNel<Rejection, ResolvedProtobufRpcSchema> {
            val ownRpcs = rpcs.toList()
            val rejections =
                ownRpcs
                    .groupingBy { it.name }
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys
                    .map { name -> Rejection.DuplicateRpcName(name) }

            return rejections
                .toNonEmptyListOrNone()
                .fold(
                    ifEmpty = { Either.Right(ResolvedProtobufRpcSchema(qualifiedName, ownRpcs)) },
                    ifSome = { Either.Left(it) },
                )
        }
    }
}
