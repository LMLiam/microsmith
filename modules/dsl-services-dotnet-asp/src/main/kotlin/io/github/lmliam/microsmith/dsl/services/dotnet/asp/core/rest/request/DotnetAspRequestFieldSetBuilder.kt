package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetConfigurableTypedFieldScope
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

internal open class DotnetAspRequestFieldSetBuilder(private val fieldContainerLabel: String) :
    DotnetConfigurableTypedFieldScope<DotnetAspRequestField, DotnetAspRequestFieldScope> {
    private val fieldsByName = linkedMapOf<String, DotnetAspRequestField>()

    override fun string(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.String, block)

    override fun char(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.Char, block)

    override fun byte(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.Byte, block)

    override fun sbyte(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.SignedByte, block)

    override fun short(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.Short, block)

    override fun ushort(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.UnsignedShort, block)

    override fun int(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.Int, block)

    override fun uint(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.UnsignedInt, block)

    override fun long(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.Long, block)

    override fun ulong(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.UnsignedLong, block)

    override fun nint(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.NativeInt, block)

    override fun nuint(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.UnsignedNativeInt, block)

    override fun float(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.Float, block)

    override fun double(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.Double, block)

    override fun decimal(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.Decimal, block)

    override fun bool(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.Bool, block)

    override fun guid(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.Guid, block)

    override fun dateOnly(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.DateOnly, block)

    override fun timeOnly(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.TimeOnly, block)

    override fun dateTime(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.DateTime, block)

    override fun dateTimeOffset(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.DateTimeOffset, block)

    override fun timeSpan(name: String, block: DotnetAspRequestFieldScope.() -> Unit) =
        registerField(name, DotnetFieldType.TimeSpan, block)

    override infix fun String.ref(target: String) = registerReference(this, target)

    override infix fun String.references(target: String) = registerReference(this, target)

    protected fun buildFields(): List<DotnetAspRequestField> = fieldsByName.values.toList()

    protected open fun createField(
        name: String,
        type: DotnetFieldType,
        options: DotnetAspRequestFieldOptions,
    ): DotnetAspRequestField = DotnetAspRequestField(
        name = name,
        type = type,
        optional = options.optional,
        defaultValue = requireCompatibleDotnetAspDefaultValue(type, options.defaultValue),
    )

    protected open fun createReference(name: String, target: String): DotnetAspRequestField =
        DotnetAspRequestField(name = name, type = DotnetFieldType.Reference(target))

    private fun registerField(
        name: String,
        type: DotnetFieldType,
        block: DotnetAspRequestFieldScope.() -> Unit,
    ): DotnetAspRequestField {
        val fieldName = validateDotnetIdentifier(name, "ASP.NET request field name")
        val options = DotnetAspRequestFieldOptions().apply(block)
        return register(createField(fieldName, type, options))
    }

    private fun registerReference(name: String, target: String): DotnetAspRequestField {
        val fieldName = validateDotnetIdentifier(name, "ASP.NET request field name")
        return register(createReference(fieldName, target))
    }

    private fun register(field: DotnetAspRequestField): DotnetAspRequestField {
        require(field.name !in fieldsByName) {
            "Duplicate ASP.NET request field '${field.name}' in $fieldContainerLabel."
        }
        fieldsByName[field.name] = field
        return field
    }
}
