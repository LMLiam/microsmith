package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

class ProtobufSymbolTable(symbols: Collection<Symbol>) {
    data class Symbol(val identity: QualifiedSchemaName, val kind: ProtobufDeclarationKind)

    sealed interface LookupFailure {
        data class InvalidName(val issue: ProtobufResolutionIssue.InvalidSchemaName) : LookupFailure

        data class Unknown(val target: QualifiedSchemaName) : LookupFailure
    }

    private val symbols = symbols.associateBy { it.identity.fullyQualifiedName }

    private fun find(identity: QualifiedSchemaName): Symbol? = symbols[identity.fullyQualifiedName]

    fun lookup(from: QualifiedSchemaName, reference: Reference): Either<LookupFailure, Symbol> =
        from
            .resolveReference(reference)
            .mapLeft { LookupFailure.InvalidName(it) }
            .flatMap { target -> find(target)?.right() ?: LookupFailure.Unknown(target).left() }
}
