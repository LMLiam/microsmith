package io.github.lmliam.microsmith.dsl.services.dotnet.core.model

import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

open class DotnetFieldSetBuilder(
    private val fieldNameLabel: String = "Field name",
    private val duplicateFieldMessage: (String) -> String = { fieldName ->
        "Duplicate .NET field registration for '$fieldName'."
    },
) : DotnetTypedFieldScope<DotnetField> {
    private val fieldsByName = linkedMapOf<String, DotnetField>()

    override fun string(name: String) = addField(name, DotnetFieldType.String)

    override fun char(name: String) = addField(name, DotnetFieldType.Char)

    override fun byte(name: String) = addField(name, DotnetFieldType.Byte)

    override fun sbyte(name: String) = addField(name, DotnetFieldType.SignedByte)

    override fun short(name: String) = addField(name, DotnetFieldType.Short)

    override fun ushort(name: String) = addField(name, DotnetFieldType.UnsignedShort)

    override fun int(name: String) = addField(name, DotnetFieldType.Int)

    override fun uint(name: String) = addField(name, DotnetFieldType.UnsignedInt)

    override fun long(name: String) = addField(name, DotnetFieldType.Long)

    override fun ulong(name: String) = addField(name, DotnetFieldType.UnsignedLong)

    override fun nint(name: String) = addField(name, DotnetFieldType.NativeInt)

    override fun nuint(name: String) = addField(name, DotnetFieldType.UnsignedNativeInt)

    override fun float(name: String) = addField(name, DotnetFieldType.Float)

    override fun double(name: String) = addField(name, DotnetFieldType.Double)

    override fun decimal(name: String) = addField(name, DotnetFieldType.Decimal)

    override fun bool(name: String) = addField(name, DotnetFieldType.Bool)

    override fun guid(name: String) = addField(name, DotnetFieldType.Guid)

    override fun dateOnly(name: String) = addField(name, DotnetFieldType.DateOnly)

    override fun timeOnly(name: String) = addField(name, DotnetFieldType.TimeOnly)

    override fun dateTime(name: String) = addField(name, DotnetFieldType.DateTime)

    override fun dateTimeOffset(name: String) = addField(name, DotnetFieldType.DateTimeOffset)

    override fun timeSpan(name: String) = addField(name, DotnetFieldType.TimeSpan)

    override infix fun String.ref(target: String) = addReference(this, target)

    override infix fun String.references(target: String) = this ref target

    protected fun buildFields(): List<DotnetField> = fieldsByName.values.toList()

    protected fun addField(name: String, type: DotnetFieldType): DotnetField {
        val fieldName = validateDotnetIdentifier(name, fieldNameLabel)
        require(fieldName !in fieldsByName) {
            duplicateFieldMessage(fieldName)
        }

        val field = DotnetField(name = fieldName, type = type)
        fieldsByName[fieldName] = field
        return field
    }

    protected fun addReference(name: String, target: String): DotnetField =
        addField(name, DotnetFieldType.Reference(target))
}
