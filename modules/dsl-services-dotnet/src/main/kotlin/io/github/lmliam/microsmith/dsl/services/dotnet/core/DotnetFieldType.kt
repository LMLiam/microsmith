package io.github.lmliam.microsmith.dsl.services.dotnet.core

/**
 * Supported field types for service-local .NET models.
 */
sealed class DotnetFieldType(val csharpType: String) {
    data object StringType : DotnetFieldType("string")

    data object IntType : DotnetFieldType("int")

    data object LongType : DotnetFieldType("long")

    data object BoolType : DotnetFieldType("bool")

    data class Reference(val target: String) : DotnetFieldType(target) {
        init {
            validateDotnetIdentifier(target, "Reference target")
        }
    }

    override fun toString(): String = csharpType
}
