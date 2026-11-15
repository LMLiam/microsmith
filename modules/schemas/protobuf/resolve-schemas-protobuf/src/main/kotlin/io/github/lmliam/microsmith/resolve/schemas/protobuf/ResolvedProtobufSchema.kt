package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

@ConsistentCopyVisibility
public data class ResolvedProtobufSchema
private constructor(
    public val identity: QualifiedSchemaName,
    public val declaration: ResolvedProtobufDeclaration,
    public val dependencies: List<QualifiedSchemaName>,
) {
    public companion object {
        public fun of(
            declaration: ResolvedProtobufDeclaration,
            packageOf: QualifiedSchemaName,
        ): ResolvedProtobufSchema {
            val identity = packageOf.withTypeName(declaration.name)

            return ResolvedProtobufSchema(
                identity,
                declaration,
                dependenciesOf(identity, declaration.references().targets()),
            )
        }
    }
}
