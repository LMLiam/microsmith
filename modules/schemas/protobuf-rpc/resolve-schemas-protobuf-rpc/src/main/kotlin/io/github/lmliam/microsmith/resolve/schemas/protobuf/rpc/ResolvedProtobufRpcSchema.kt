package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNone
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoFilePaths
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

@ConsistentCopyVisibility
data class ResolvedProtobufRpcSchema
private constructor(val qualifiedName: QualifiedSchemaName, val rpcs: List<ResolvedProtobufRpc>) {
    val imports: List<String> =
        rpcs
            .flatMap { rpc -> listOf(rpc.request.target, rpc.response.target) }
            .filter { target -> target != qualifiedName }
            .map { target -> ProtoFilePaths.importPath(target.fullyQualifiedName) }
            .distinct()
            .sorted()

    sealed interface Rejection {
        data class DuplicateRpcName(val name: ProtoIdentifier) : Rejection
    }

    companion object {
        fun of(
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
