package io.github.lmliam.microsmith.dsl.services.dotnet.core.model

import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

/**
 * Supported field types for service-local .NET models.
 */
sealed class DotnetFieldType(val csharpType: kotlin.String) {
    data object String : DotnetFieldType("string")

    data object Char : DotnetFieldType("char")

    data object Byte : DotnetFieldType("byte")

    data object SByte : DotnetFieldType("sbyte")

    data object Short : DotnetFieldType("short")

    data object UShort : DotnetFieldType("ushort")

    data object Int : DotnetFieldType("int")

    data object UInt : DotnetFieldType("uint")

    data object Long : DotnetFieldType("long")

    data object ULong : DotnetFieldType("ulong")

    data object NInt : DotnetFieldType("nint")

    data object NUInt : DotnetFieldType("nuint")

    data object Float : DotnetFieldType("float")

    data object Double : DotnetFieldType("double")

    data object Decimal : DotnetFieldType("decimal")

    data object Bool : DotnetFieldType("bool")

    data object Guid : DotnetFieldType("Guid")

    data object DateOnly : DotnetFieldType("DateOnly")

    data object TimeOnly : DotnetFieldType("TimeOnly")

    data object DateTime : DotnetFieldType("DateTime")

    data object DateTimeOffset : DotnetFieldType("DateTimeOffset")

    data object TimeSpan : DotnetFieldType("TimeSpan")

    data class Reference(val target: kotlin.String) : DotnetFieldType(target) {
        init {
            validateDotnetIdentifier(target, "Reference target")
        }
    }

    override fun toString(): kotlin.String = csharpType
}
