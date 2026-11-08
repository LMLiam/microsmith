package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal class ProtobufSymbolTable(symbols: Collection<Symbol>) {
    data class Symbol(val identity: QualifiedSchemaName, val kind: ProtobufDeclarationKind)

    private val symbols = symbols.associateBy { it.identity.fullyQualifiedName }

    fun find(identity: QualifiedSchemaName): Symbol? = symbols[identity.fullyQualifiedName]
}
