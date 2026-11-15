package io.github.lmliam.microsmith.dsl.services.dotnet.model

import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

/** Supported field types for service-local .NET models. */
public sealed class DotnetFieldType(public val csharpType: kotlin.String) {
    public data object String : DotnetFieldType("string")

    public data object Char : DotnetFieldType("char")

    public data object Byte : DotnetFieldType("byte")

    public data object SignedByte : DotnetFieldType("sbyte")

    public data object Short : DotnetFieldType("short")

    public data object UnsignedShort : DotnetFieldType("ushort")

    public data object Int : DotnetFieldType("int")

    public data object UnsignedInt : DotnetFieldType("uint")

    public data object Long : DotnetFieldType("long")

    public data object UnsignedLong : DotnetFieldType("ulong")

    public data object NativeInt : DotnetFieldType("nint")

    public data object UnsignedNativeInt : DotnetFieldType("nuint")

    public data object Float : DotnetFieldType("float")

    public data object Double : DotnetFieldType("double")

    public data object Decimal : DotnetFieldType("decimal")

    public data object Bool : DotnetFieldType("bool")

    public data object Guid : DotnetFieldType("Guid")

    public data object DateOnly : DotnetFieldType("DateOnly")

    public data object TimeOnly : DotnetFieldType("TimeOnly")

    public data object DateTime : DotnetFieldType("DateTime")

    public data object DateTimeOffset : DotnetFieldType("DateTimeOffset")

    public data object TimeSpan : DotnetFieldType("TimeSpan")

    public data class Reference(public val target: kotlin.String) : DotnetFieldType(target) {
        init {
            validateDotnetIdentifier(target, "Reference target")
        }
    }

    override fun toString(): kotlin.String = csharpType
}
