package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufRejection
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchema
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal fun findMessageSymbolCollisions(
    fieldNames: List<String>,
    oneofNames: List<String>,
    mapFieldNames: List<String>,
): List<ResolvedProtobufRejection> =
    findSymbolClashes(messageSymbols(fieldNames, oneofNames, mapFieldNames)).map {
        ResolvedProtobufRejection.MessageSymbolCollision(it.name, it.declarations)
    }

internal fun findPackageSymbolCollisions(
    schemas: List<ResolvedProtobufSchema>,
): List<ResolvedProtobufSchemaModel.Rejection> =
    findSymbolClashes(
            schemas.flatMap { schema ->
                packageScopeSymbols(
                    schema.identity,
                    when (val declaration = schema.declaration) {
                        is ResolvedProtobufEnum -> declaration.values.map { it.name.value }
                        is ResolvedProtobufMessage -> emptyList()
                    },
                )
            },
        )
        .map { ResolvedProtobufSchemaModel.Rejection.PackageSymbolCollision(it.scope, it.name, it.declarations) }

internal fun packageScopeSymbols(
    identity: QualifiedSchemaName,
    enumValueNames: List<String>,
): List<ScopedSymbol<String, ProtobufResolutionIssue.SymbolDeclaration>> {
    val packageName = identity.packageName.orEmpty()
    val schemaName = identity.fullyQualifiedName
    val segments = identity.packageName?.split('.').orEmpty()

    return segments.indices.map { index ->
        ScopedSymbol(
            segments.take(index).joinToString("."),
            segments[index],
            ProtobufResolutionIssue.SymbolDeclaration.Package(segments.take(index + 1).joinToString(".")),
        )
    } +
        ScopedSymbol(
            packageName,
            identity.typeName.value,
            ProtobufResolutionIssue.SymbolDeclaration.Type(schemaName),
        ) +
        enumValueNames.map {
            ScopedSymbol(packageName, it, ProtobufResolutionIssue.SymbolDeclaration.EnumValue(schemaName, it))
        }
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
    symbols: List<ScopedSymbol<Scope, Declaration>>,
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
