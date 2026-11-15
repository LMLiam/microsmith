package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.Either
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal class ProtobufReferenceResolver
@AssistedInject
constructor(@Assisted private val symbols: ProtobufSymbolTable) {
    context(identity: QualifiedSchemaName)
    fun resolve(
        reference: Reference,
        location: ProtobufResolutionIssue.ReferenceLocation,
    ): Either<ProtobufResolutionIssue, ResolvedProtobufReference> =
        symbols
            .lookup(identity, reference)
            .mapLeft { failure ->
                when (failure) {
                    is ProtobufSymbolTable.LookupFailure.InvalidName ->
                        ProtobufResolutionIssue.InvalidReferenceName(
                            identity.fullyQualifiedName,
                            location,
                            failure.issue.schemaName,
                            failure.issue.reason,
                        )

                    is ProtobufSymbolTable.LookupFailure.Unknown ->
                        ProtobufResolutionIssue.UnresolvedReference(
                            identity.fullyQualifiedName,
                            location,
                            failure.target.fullyQualifiedName,
                        )
                }
            }
            .map { symbol -> ResolvedProtobufReference(symbol.identity, symbol.kind) }

    @AssistedFactory
    fun interface Factory {
        fun create(symbols: ProtobufSymbolTable): ProtobufReferenceResolver
    }
}
