package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue.SymbolDeclaration
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal fun findSymbolCollisions(schemas: List<ProtobufSchema>): List<ProtobufResolutionIssue.SymbolNameCollision> =
    schemas
        .sortedBy(ProtobufSchema::name)
        .flatMap(::scopedSymbolsOf)
        .distinct()
        .groupBy { it.scope to it.name }
        .values
        .filter { it.size > 1 }
        .map { symbols ->
            ProtobufResolutionIssue.SymbolNameCollision(
                scope = symbols.first().scope.name,
                name = symbols.first().name,
                declarations = symbols.map(ScopedSymbol::declaration),
            )
        }

private data class Scope(val kind: Kind, val name: String) {
    enum class Kind {
        PACKAGE,
        MESSAGE,
    }
}

private data class ScopedSymbol(val scope: Scope, val name: String, val declaration: SymbolDeclaration)

private fun scopedSymbolsOf(schema: ProtobufSchema): List<ScopedSymbol> =
    QualifiedSchemaName.parse(schema.name)
        .fold(
            ifLeft = { emptyList() },
            ifRight = { identity -> packageSymbols(identity) + declarationSymbols(identity, schema.schema) },
        )

private fun packageSymbols(identity: QualifiedSchemaName): List<ScopedSymbol> {
    val segments = identity.packageName?.split('.').orEmpty()

    return segments.indices.map { index ->
        ScopedSymbol(
            Scope(Scope.Kind.PACKAGE, segments.take(index).joinToString(".")),
            segments[index],
            SymbolDeclaration.Package(segments.take(index + 1).joinToString(".")),
        )
    }
}

private fun declarationSymbols(identity: QualifiedSchemaName, declaration: Type): List<ScopedSymbol> {
    val packageScope = Scope(Scope.Kind.PACKAGE, identity.packageName.orEmpty())
    val schemaName = identity.fullyQualifiedName
    val type = ScopedSymbol(packageScope, identity.typeName.value, SymbolDeclaration.Type(schemaName))

    return listOf(type) +
        when (declaration) {
            is Message -> messageSymbols(schemaName, declaration)

            is Enum ->
                declaration.values
                    .map { it.name }
                    .filter(::isIdentifier)
                    .map { ScopedSymbol(packageScope, it, SymbolDeclaration.EnumValue(schemaName, it)) }

            else -> emptyList()
        }
}

private fun messageSymbols(schemaName: String, message: Message): List<ScopedSymbol> {
    val scope = Scope(Scope.Kind.MESSAGE, schemaName)
    val fields = message.fields + message.oneofs.flatMap(Oneof::fields)

    return fields.map(Field::name).filter(::isIdentifier).map {
        ScopedSymbol(scope, it, SymbolDeclaration.Field(schemaName, it))
    } +
        message.oneofs.map(Oneof::name).filter(::isIdentifier).map {
            ScopedSymbol(scope, it, SymbolDeclaration.Oneof(schemaName, it))
        } +
        message.fields.filterIsInstance<MapField>().map(MapField::name).filter(::isIdentifier).map {
            ScopedSymbol(scope, mapEntryName(it), SymbolDeclaration.MapEntry(schemaName, it))
        }
}

private fun isIdentifier(raw: String): Boolean = ProtoIdentifier.parse(raw).isRight()

private fun mapEntryName(fieldName: String): String {
    val camelCase =
        fieldName.split('_').joinToString("") { part ->
            part.replaceFirstChar { if (it in 'a'..'z') it.uppercaseChar() else it }
        }

    return camelCase + "Entry"
}
