package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

import arrow.core.Either
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import kotlin.io.path.Path

data class QualifiedSchemaName(val fullyQualifiedName: String, val packageName: String?, val typeName: String) {
    fun resolveReference(reference: Reference): Either<ProtobufResolutionIssue.InvalidSchemaName, QualifiedSchemaName> =
        when (reference) {
            is Reference.Local -> parse(packageName?.let { "$it.${reference.target}" } ?: reference.target)
            is Reference.Relative -> {
                val upCount = reference.expression.takeWhile { it == '.' }.length
                val remaining = reference.expression.drop(upCount)
                val currentPackage = packageName?.split('.').orEmpty()
                val target = currentPackage.dropLast(upCount.coerceAtMost(currentPackage.size)) + remaining.split('.')

                parse(target.joinToString("."))
            }
            is Reference.Qualified -> parse(reference.qualifiedName)
            is Reference.Symbolic -> parse(reference.target.qualifiedName)
        }

    fun relativePath(): java.nio.file.Path = buildRelativePath(prefix = Path("proto"))

    fun buildRelativePath(prefix: java.nio.file.Path): java.nio.file.Path =
        packageName?.replace(".", "/")?.let { packagePath -> prefix.resolve(packagePath).resolve("$typeName.proto") }
            ?: prefix.resolve("$typeName.proto")

    companion object {
        fun parse(rawName: String): Either<ProtobufResolutionIssue.InvalidSchemaName, QualifiedSchemaName> =
            ProtobufNameValidation.normalizeQualifiedName(rawName).map { normalized ->
                val packageName = normalized.substringBeforeLast(".", "").ifBlank { null }
                val typeName = normalized.substringAfterLast(".")

                QualifiedSchemaName(
                    fullyQualifiedName = normalized,
                    packageName = packageName,
                    typeName = typeName,
                )
            }
    }
}
