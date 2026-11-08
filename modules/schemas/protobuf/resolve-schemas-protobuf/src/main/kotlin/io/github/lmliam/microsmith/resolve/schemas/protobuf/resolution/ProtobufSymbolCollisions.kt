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
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing.declaredIn
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.ScopedSymbol
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.findSymbolClashes
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.messageSymbols

internal fun findSymbolCollisions(schemas: List<ProtobufSchema>): List<ProtobufResolutionIssue.SymbolNameCollision> =
    findSymbolClashes(schemas.sortedBy(ProtobufSchema::name).flatMap(::scopedSymbolsOf)).map { clash ->
        ProtobufResolutionIssue.SymbolNameCollision(
            scope = clash.scope.name,
            name = clash.name,
            declarations = clash.declarations,
        )
    }

private data class Scope(val kind: Kind, val name: String) {
    enum class Kind {
        PACKAGE,
        MESSAGE,
    }
}

private typealias PackageOrMessageSymbol = ScopedSymbol<Scope, SymbolDeclaration>

private fun scopedSymbolsOf(schema: ProtobufSchema): List<PackageOrMessageSymbol> =
    QualifiedSchemaName.parse(schema.name)
        .fold(
            ifLeft = { emptyList() },
            ifRight = { identity -> packageSymbols(identity) + declarationSymbols(identity, schema.schema) },
        )

private fun packageSymbols(identity: QualifiedSchemaName): List<PackageOrMessageSymbol> {
    val segments = identity.packageName?.split('.').orEmpty()

    return segments.indices.map { index ->
        ScopedSymbol(
            Scope(Scope.Kind.PACKAGE, segments.take(index).joinToString(".")),
            segments[index],
            SymbolDeclaration.Package(segments.take(index + 1).joinToString(".")),
        )
    }
}

private fun declarationSymbols(identity: QualifiedSchemaName, declaration: Type): List<PackageOrMessageSymbol> {
    val packageScope = Scope(Scope.Kind.PACKAGE, identity.packageName.orEmpty())
    val schemaName = identity.fullyQualifiedName
    val type = ScopedSymbol(packageScope, identity.typeName.value, SymbolDeclaration.Type(schemaName))

    return listOf(type) +
        when (declaration) {
            is Message -> scopedMessageSymbols(schemaName, declaration)

            is Enum ->
                declaration.values
                    .map { it.name }
                    .filter(::isIdentifier)
                    .map { ScopedSymbol(packageScope, it, SymbolDeclaration.EnumValue(schemaName, it)) }

            else -> emptyList()
        }
}

private fun scopedMessageSymbols(schemaName: String, message: Message): List<PackageOrMessageSymbol> {
    val scope = Scope(Scope.Kind.MESSAGE, schemaName)

    return messageSymbols(
            fieldNames =
                (message.fields + message.oneofs.flatMap(Oneof::fields)).map(Field::name).filter(::isIdentifier),
            oneofNames = message.oneofs.map(Oneof::name).filter(::isIdentifier),
            mapFieldNames = message.fields.filterIsInstance<MapField>().map(MapField::name).filter(::isIdentifier),
        )
        .map { ScopedSymbol(scope, it.name, it.declaration.declaredIn(schemaName)) }
}
