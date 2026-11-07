package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.Either
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal class ProtobufReferenceResolver(private val symbols: ProtobufSymbolTable) {
    fun resolve(
        schema: QualifiedSchemaName,
        reference: Reference,
        location: ProtobufResolutionIssue.ReferenceLocation,
    ): Either<ProtobufResolutionIssue, ResolvedProtobufReference> {
        val target =
            when (val resolved = schema.resolveReference(reference)) {
                is Either.Left -> return Either.Left(resolved.value)

                is Either.Right -> resolved.value
            }

        val symbol =
            symbols.find(target)
                ?: return Either.Left(
                    ProtobufResolutionIssue.UnresolvedReference(
                        schema.fullyQualifiedName,
                        location,
                        target.fullyQualifiedName,
                    )
                )

        return Either.Right(ResolvedProtobufReference(symbol.identity, symbol.kind))
    }
}
