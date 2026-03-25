package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

import kotlin.io.path.Path

/**
 * Parsed protobuf schema name with separated package/type segments.
 */
data class QualifiedSchemaName(
    val fullyQualifiedName: String,
    val packageName: String?,
    val typeName: String,
) {
    fun relativePath(): java.nio.file.Path = buildRelativePath(prefix = Path("proto"))

    fun buildRelativePath(prefix: java.nio.file.Path): java.nio.file.Path = packageName
        ?.replace(".", "/")
        ?.let { packagePath -> prefix.resolve(packagePath).resolve("$typeName.proto") }
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
