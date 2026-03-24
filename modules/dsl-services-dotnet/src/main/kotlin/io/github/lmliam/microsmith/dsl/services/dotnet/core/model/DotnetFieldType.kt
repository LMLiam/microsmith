package io.github.lmliam.microsmith.dsl.services.dotnet.core.model

import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

/**
 * Supported field types for service-local .NET models.
 */
sealed class DotnetFieldType(val csharpType: String) {
    data object StringType : DotnetFieldType("string")

    data object CharType : DotnetFieldType("char")

    data object ByteType : DotnetFieldType("byte")

    data object ShortType : DotnetFieldType("short")

    data object IntType : DotnetFieldType("int")

    data object LongType : DotnetFieldType("long")

    data object FloatType : DotnetFieldType("float")

    data object DoubleType : DotnetFieldType("double")

    data object DecimalType : DotnetFieldType("decimal")

    data object BoolType : DotnetFieldType("bool")

    data object GuidType : DotnetFieldType("Guid")

    data object DateOnlyType : DotnetFieldType("DateOnly")

    data object TimeOnlyType : DotnetFieldType("TimeOnly")

    data object DateTimeType : DotnetFieldType("DateTime")

    data object DateTimeOffsetType : DotnetFieldType("DateTimeOffset")

    data object TimeSpanType : DotnetFieldType("TimeSpan")

    data class Reference(val target: String) : DotnetFieldType(target) {
        init {
            validateDotnetIdentifier(target, "Reference target")
        }
    }

    override fun toString(): String = csharpType
}
