package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.Either
import arrow.core.raise.context.bind
import arrow.core.raise.context.either
import arrow.core.raise.context.raise
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal class ProtobufReferenceResolver(private val symbols: ProtobufSymbolTable) {
    fun resolve(
        schema: QualifiedSchemaName,
        reference: Reference,
        location: ProtobufResolutionIssue.ReferenceLocation,
    ): Either<ProtobufResolutionIssue, ResolvedProtobufReference> = either {
        val target =
            schema
                .resolveReference(reference)
                .mapLeft { invalid ->
                    ProtobufResolutionIssue.InvalidReferenceName(
                        schema.fullyQualifiedName,
                        location,
                        invalid.schemaName,
                        invalid.reason,
                    )
                }
                .bind()

        val symbol =
            symbols.find(target)
                ?: raise(
                    ProtobufResolutionIssue.UnresolvedReference(
                        schema.fullyQualifiedName,
                        location,
                        target.fullyQualifiedName,
                    )
                )

        ResolvedProtobufReference(symbol.identity, symbol.kind)
    }
}
