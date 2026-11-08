package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufRejection

internal fun findMessageSymbolCollisions(
    fieldNames: List<String>,
    oneofNames: List<String>,
    mapFieldNames: List<String>,
): List<ResolvedProtobufRejection> =
    findSymbolClashes(messageSymbols(fieldNames, oneofNames, mapFieldNames)).map {
        ResolvedProtobufRejection.MessageSymbolCollision(it.name, it.declarations)
    }

internal fun messageSymbols(
    fieldNames: List<String>,
    oneofNames: List<String>,
    mapFieldNames: List<String>,
): List<ScopedSymbol<Unit, ResolvedProtobufRejection.MessageSymbol>> =
    fieldNames.map { ScopedSymbol(Unit, it, ResolvedProtobufRejection.MessageSymbol.Field(it)) } +
        oneofNames.map { ScopedSymbol(Unit, it, ResolvedProtobufRejection.MessageSymbol.Oneof(it)) } +
        mapFieldNames.map { ScopedSymbol(Unit, mapEntryName(it), ResolvedProtobufRejection.MessageSymbol.MapEntry(it)) }

internal fun <Scope, Declaration> findSymbolClashes(
    symbols: List<ScopedSymbol<Scope, Declaration>>
): List<SymbolClash<Scope, Declaration>> =
    symbols
        .distinct()
        .groupBy { it.scope to it.name }
        .values
        .filter { it.size > 1 }
        .map { group ->
            SymbolClash(
                group.first().scope,
                group.first().name,
                group.map(ScopedSymbol<Scope, Declaration>::declaration),
            )
        }

private fun mapEntryName(fieldName: String): String = fieldName.underscoresToCamelCase(capitalizeFirst = true) + "Entry"
