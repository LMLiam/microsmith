package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

@ConsistentCopyVisibility
data class ResolvedProtobufSchema
private constructor(
    val identity: QualifiedSchemaName,
    val declaration: ResolvedProtobufDeclaration,
    val dependencies: List<QualifiedSchemaName>,
) {
    companion object {
        fun of(declaration: ResolvedProtobufDeclaration, packageOf: QualifiedSchemaName): ResolvedProtobufSchema {
            val identity = packageOf.withTypeName(declaration.name)

            return ResolvedProtobufSchema(
                identity,
                declaration,
                dependenciesOf(identity, declaration.references().targets()),
            )
        }
    }
}
