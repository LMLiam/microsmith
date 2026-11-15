package io.github.lmliam.microsmith.dsl.services.dotnet.model

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
public interface DotnetConfigurableTypedFieldScope<TField, TOptionsScope> {
    public fun string(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun char(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun byte(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun sbyte(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun short(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun ushort(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun int(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun uint(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun long(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun ulong(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun nint(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun nuint(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun float(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun double(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun decimal(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun bool(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun guid(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun dateOnly(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun timeOnly(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun dateTime(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun dateTimeOffset(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public fun timeSpan(name: String, block: TOptionsScope.() -> Unit = {}): TField

    public infix fun String.ref(target: String): TField

    public infix fun String.references(target: String): TField
}
