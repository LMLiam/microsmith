package io.github.lmliam.microsmith.gen.schemas.protobuf.names

import kotlin.io.path.Path

/**
 * Parsed protobuf schema name with separated package/type segments.
 *
 * Invariants:
 * - [fullyQualifiedName] is normalized and non-blank.
 * - [typeName] is non-blank.
 * - [packageName] is `null` for unqualified schema names.
 */
data class QualifiedSchemaName(
    val fullyQualifiedName: String,
    val packageName: String?,
    val typeName: String,
) {
    /** Returns the canonical relative output path under `proto/`. */
    fun relativePath(): java.nio.file.Path {
        val packagePath =
            packageName
                ?.replace(".", "/")
                ?.let { "$it/" }
                .orEmpty()
        return Path("proto/$packagePath$typeName.proto")
    }

    companion object {
        /** Parses and validates a raw schema name
         * from [io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema.name].
         */
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
