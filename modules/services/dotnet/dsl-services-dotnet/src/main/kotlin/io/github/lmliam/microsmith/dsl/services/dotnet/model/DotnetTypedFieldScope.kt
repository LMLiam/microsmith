package io.github.lmliam.microsmith.dsl.services.dotnet.model

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
public interface DotnetTypedFieldScope<TField> {
    public fun string(name: String): TField

    public fun char(name: String): TField

    public fun byte(name: String): TField

    public fun sbyte(name: String): TField

    public fun short(name: String): TField

    public fun ushort(name: String): TField

    public fun int(name: String): TField

    public fun uint(name: String): TField

    public fun long(name: String): TField

    public fun ulong(name: String): TField

    public fun nint(name: String): TField

    public fun nuint(name: String): TField

    public fun float(name: String): TField

    public fun double(name: String): TField

    public fun decimal(name: String): TField

    public fun bool(name: String): TField

    public fun guid(name: String): TField

    public fun dateOnly(name: String): TField

    public fun timeOnly(name: String): TField

    public fun dateTime(name: String): TField

    public fun dateTimeOffset(name: String): TField

    public fun timeSpan(name: String): TField

    public infix fun String.ref(target: String): TField

    public infix fun String.references(target: String): TField
}
