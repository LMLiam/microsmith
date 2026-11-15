package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

@InternalMicrosmithApi
public class ProtobufSymbolTable(symbols: Collection<Symbol>) {
    @InternalMicrosmithApi
    public data class Symbol(public val identity: QualifiedSchemaName, public val kind: ProtobufDeclarationKind)

    @InternalMicrosmithApi
    public sealed interface LookupFailure {
        @InternalMicrosmithApi
        public data class InvalidName(public val issue: ProtobufResolutionIssue.InvalidSchemaName) : LookupFailure

        @InternalMicrosmithApi public data class Unknown(public val target: QualifiedSchemaName) : LookupFailure
    }

    private val symbols = symbols.associateBy { it.identity.fullyQualifiedName }

    private fun find(identity: QualifiedSchemaName): Symbol? = symbols[identity.fullyQualifiedName]

    public fun lookup(from: QualifiedSchemaName, reference: Reference): Either<LookupFailure, Symbol> =
        from
            .resolveReference(reference)
            .mapLeft { LookupFailure.InvalidName(it) }
            .flatMap { target -> find(target)?.right() ?: LookupFailure.Unknown(target).left() }
}
