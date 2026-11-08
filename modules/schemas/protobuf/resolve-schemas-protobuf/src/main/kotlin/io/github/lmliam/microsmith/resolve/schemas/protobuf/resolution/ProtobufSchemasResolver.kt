package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.nonEmptyListOf
import arrow.core.separateEither
import arrow.core.toNonEmptyListOrNull
import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

@ServiceProvider(DomainResolver::class)
class ProtobufSchemasResolver : DomainResolver<SchemasExtension, ResolvedProtobufSchemaModel> {
    override val authoringType = SchemasExtension::class
    override val resolvedType = ResolvedProtobufSchemaModel::class

    override fun resolve(authoring: SchemasExtension): DomainResolution<ResolvedProtobufSchemaModel> {
        val protobufSchemas = authoring.schemas.filterIsInstance<ProtobufSchema>()
        val declared = protobufSchemas.mapNotNull(::declaredSchemaOf)

        val (_, symbols) = declared.map { it.symbol() }.separateEither()
        val resolver = ProtobufDeclarationResolver(ProtobufReferenceResolver(ProtobufSymbolTable(symbols)))
        val issues = mutableListOf<ProtobufResolutionIssue>()
        val resolved =
            declared
                .sortedBy { it.schema.name }
                .mapNotNull { declaredSchema ->
                    declaredSchema.identity
                        .mapLeft { invalid -> nonEmptyListOf<ProtobufResolutionIssue>(invalid) }
                        .flatMap { identity ->
                            when (declaredSchema) {
                                is DeclaredSchema.OfMessage -> resolver.resolveMessage(identity, declaredSchema.message)
                                is DeclaredSchema.OfEnum -> resolver.resolveEnum(identity, declaredSchema.declaration)
                            }
                        }
                        .fold(
                            ifLeft = { failures ->
                                issues.addAll(failures)
                                null
                            },
                            ifRight = { it },
                        )
                }

        issues.addAll(findSymbolCollisions(protobufSchemas))

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        return when {
            accumulatedIssues != null -> DomainResolution.Failure(accumulatedIssues)
            resolved.isEmpty() -> DomainResolution.NotApplicable
            else -> DomainResolution.Success(ResolvedProtobufSchemaModel(resolved))
        }
    }
}

private sealed interface DeclaredSchema {
    val schema: ProtobufSchema
    val identity: Either<ProtobufResolutionIssue.InvalidSchemaName, QualifiedSchemaName>
    val kind: ProtobufDeclarationKind

    class OfMessage(
        override val schema: ProtobufSchema,
        val message: Message,
        override val identity: Either<ProtobufResolutionIssue.InvalidSchemaName, QualifiedSchemaName>,
    ) : DeclaredSchema {
        override val kind = ProtobufDeclarationKind.MESSAGE
    }

    class OfEnum(
        override val schema: ProtobufSchema,
        val declaration: Enum,
        override val identity: Either<ProtobufResolutionIssue.InvalidSchemaName, QualifiedSchemaName>,
    ) : DeclaredSchema {
        override val kind = ProtobufDeclarationKind.ENUM
    }

    fun symbol(): Either<ProtobufResolutionIssue.InvalidSchemaName, ProtobufSymbolTable.Symbol> = identity.map {
        ProtobufSymbolTable.Symbol(it, kind)
    }
}

private fun declaredSchemaOf(schema: ProtobufSchema): DeclaredSchema? =
    when (val declaration = schema.schema) {
        is Message -> DeclaredSchema.OfMessage(schema, declaration, QualifiedSchemaName.parse(schema.name))
        is Enum -> DeclaredSchema.OfEnum(schema, declaration, QualifiedSchemaName.parse(schema.name))
        else -> null
    }
