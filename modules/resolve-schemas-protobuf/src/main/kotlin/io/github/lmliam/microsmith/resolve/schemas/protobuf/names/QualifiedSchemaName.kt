package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import kotlin.io.path.Path

/** Parsed protobuf schema name with separated package/type segments. */
data class QualifiedSchemaName(val fullyQualifiedName: String, val packageName: String?, val typeName: String) {
    /** Resolves a reference from this schema's package. Qualified and symbolic names remain absolute. */
    fun resolveReference(reference: Reference): QualifiedSchemaName =
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
        fun parse(rawName: String): QualifiedSchemaName {
            val normalized = ProtobufNameValidation.normalizeQualifiedName(rawName, "Schema name")
            val packageName = normalized.substringBeforeLast(".", "").ifBlank { null }
            val typeName = normalized.substringAfterLast(".")

            require(typeName.isNotBlank()) { "Schema name must include a type segment: '$rawName'" }

            return QualifiedSchemaName(
                fullyQualifiedName = normalized,
                packageName = packageName,
                typeName = typeName,
            )
        }
    }
}
