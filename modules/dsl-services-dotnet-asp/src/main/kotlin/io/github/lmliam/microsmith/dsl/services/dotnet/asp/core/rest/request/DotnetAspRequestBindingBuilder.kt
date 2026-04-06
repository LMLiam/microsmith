package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

internal class DotnetAspRequestBindingBuilder(
    private val name: String,
) : DotnetAspRequestBindingScope {
    private val fieldsByName = linkedMapOf<String, DotnetAspRequestField>()

    override fun string(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.String, block)

    override fun char(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.Char, block)

    override fun byte(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.Byte, block)

    override fun sbyte(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.SignedByte, block)

    override fun short(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.Short, block)

    override fun ushort(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.UnsignedShort, block)

    override fun int(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.Int, block)

    override fun uint(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.UnsignedInt, block)

    override fun long(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.Long, block)

    override fun ulong(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.UnsignedLong, block)

    override fun nint(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.NativeInt, block)

    override fun nuint(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.UnsignedNativeInt, block)

    override fun float(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.Float, block)

    override fun double(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.Double, block)

    override fun decimal(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.Decimal, block)

    override fun bool(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.Bool, block)

    override fun guid(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.Guid, block)

    override fun dateOnly(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.DateOnly, block)

    override fun timeOnly(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.TimeOnly, block)

    override fun dateTime(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.DateTime, block)

    override fun dateTimeOffset(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.DateTimeOffset, block)

    override fun timeSpan(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        addField(name, DotnetFieldType.TimeSpan, block)

    override infix fun String.ref(target: String) = addReference(this, target)
    override infix fun String.references(target: String) = addReference(this, target)

    fun build() = DotnetAspRequestBinding(name = name, fields = fieldsByName.values.toList())

    private fun addField(
        name: String,
        type: DotnetFieldType,
        block: DotnetAspRequestFieldScope.() -> Unit,
    ): DotnetAspRequestField {
        val options = DotnetAspRequestFieldOptions().apply(block)
        return registerField(
            DotnetAspRequestField(
                name = name,
                type = type,
                optional = options.optional,
                defaultValue = options.defaultValue,
            ),
        )
    }

    private fun addReference(name: String, target: String): DotnetAspRequestField = registerField(
        DotnetAspRequestField(name = name, type = DotnetFieldType.Reference(target)),
    )

    private fun registerField(field: DotnetAspRequestField): DotnetAspRequestField {
        require(field.name !in fieldsByName) {
            "Duplicate ASP.NET request field '${field.name}' in binding '$name'."
        }
        fieldsByName[field.name] = field
        return field
    }
}

private class DotnetAspRequestFieldOptions : DotnetAspRequestFieldScope {
    var optional = false
        private set
    var defaultValue: Any? = null
        private set

    override fun optional() {
        require(!optional) { "optional() already set for ASP.NET request field." }
        optional = true
    }

    override fun default(value: Any) {
        require(defaultValue == null) { "default(...) already set for ASP.NET request field." }
        defaultValue = value
    }
}
