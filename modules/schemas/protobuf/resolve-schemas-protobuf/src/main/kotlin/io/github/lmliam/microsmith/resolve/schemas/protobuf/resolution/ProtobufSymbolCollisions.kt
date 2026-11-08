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
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.packageScopeSymbols

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
            ifRight = { identity ->
                when (val declaration = schema.schema) {
                    is Message ->
                        packageSymbols(identity, emptyList()) +
                            scopedMessageSymbols(identity.fullyQualifiedName, declaration)

                    is Enum -> packageSymbols(identity, declaration.values.map { it.name }.filter(::isIdentifier))

                    else -> packageSymbols(identity, emptyList())
                }
            },
        )

private fun packageSymbols(identity: QualifiedSchemaName, enumValueNames: List<String>): List<PackageOrMessageSymbol> =
    packageScopeSymbols(identity, enumValueNames).map {
        ScopedSymbol(Scope(Scope.Kind.PACKAGE, it.scope), it.name, it.declaration)
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
