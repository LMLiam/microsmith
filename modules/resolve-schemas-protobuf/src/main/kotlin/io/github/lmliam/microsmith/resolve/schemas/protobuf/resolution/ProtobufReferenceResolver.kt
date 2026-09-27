package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.Either
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal class ProtobufReferenceResolver(private val symbols: ProtobufSymbolTable) {
    fun resolve(
        schema: QualifiedSchemaName,
        reference: Reference,
        location: ProtobufResolutionIssue.ReferenceLocation,
    ): Either<ProtobufResolutionIssue, ResolvedProtobufReference> {
        val target = resolveIdentity(schema, reference)
        val symbol =
            symbols.find(target)
                ?: return Either.Left(
                    ProtobufResolutionIssue.UnresolvedReference(
                        schema.fullyQualifiedName,
                        location,
                        target.fullyQualifiedName,
                    )
                )

        return Either.Right(ResolvedProtobufReference(symbol.identity, symbol.kind))
    }

    private fun resolveIdentity(current: QualifiedSchemaName, reference: Reference): QualifiedSchemaName =
        when (reference) {
            is Reference.Local ->
                QualifiedSchemaName.parse(current.packageName?.let { "$it.${reference.target}" } ?: reference.target)

            is Reference.Relative -> resolveRelative(current, reference.expression)

            is Reference.Qualified -> QualifiedSchemaName.parse(reference.qualifiedName)

            is Reference.Symbolic -> QualifiedSchemaName.parse(reference.target.qualifiedName)
        }

    private fun resolveRelative(current: QualifiedSchemaName, expression: String): QualifiedSchemaName {
        val upCount = expression.takeWhile { it == '.' }.length
        val remaining = expression.drop(upCount)
        val currentPackage = current.packageName?.split('.').orEmpty()
        val target = currentPackage.dropLast(upCount.coerceAtMost(currentPackage.size)) + remaining.split('.')

        return QualifiedSchemaName.parse(target.joinToString("."))
    }
}
