package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

internal data class ScopedSymbol<out Scope, out Declaration>(
    val scope: Scope,
    val name: String,
    val declaration: Declaration,
)

internal data class SymbolClash<Scope, Declaration>(
    val scope: Scope,
    val name: String,
    val declarations: List<Declaration>,
)
