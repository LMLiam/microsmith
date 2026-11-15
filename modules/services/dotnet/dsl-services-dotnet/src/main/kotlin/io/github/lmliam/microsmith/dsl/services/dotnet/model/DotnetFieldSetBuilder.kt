package io.github.lmliam.microsmith.dsl.services.dotnet.model

import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

@InternalMicrosmithApi
public open class DotnetFieldSetBuilder(
    private val fieldNameLabel: String = "Field name",
    private val duplicateFieldMessage: (String) -> String = { fieldName ->
        "Duplicate .NET field registration for '$fieldName'."
    },
) : DotnetTypedFieldScope<DotnetField> {
    private val fieldsByName = linkedMapOf<String, DotnetField>()

    override fun string(name: String): DotnetField = addField(name, DotnetFieldType.String)

    override fun char(name: String): DotnetField = addField(name, DotnetFieldType.Char)

    override fun byte(name: String): DotnetField = addField(name, DotnetFieldType.Byte)

    override fun sbyte(name: String): DotnetField = addField(name, DotnetFieldType.SignedByte)

    override fun short(name: String): DotnetField = addField(name, DotnetFieldType.Short)

    override fun ushort(name: String): DotnetField = addField(name, DotnetFieldType.UnsignedShort)

    override fun int(name: String): DotnetField = addField(name, DotnetFieldType.Int)

    override fun uint(name: String): DotnetField = addField(name, DotnetFieldType.UnsignedInt)

    override fun long(name: String): DotnetField = addField(name, DotnetFieldType.Long)

    override fun ulong(name: String): DotnetField = addField(name, DotnetFieldType.UnsignedLong)

    override fun nint(name: String): DotnetField = addField(name, DotnetFieldType.NativeInt)

    override fun nuint(name: String): DotnetField = addField(name, DotnetFieldType.UnsignedNativeInt)

    override fun float(name: String): DotnetField = addField(name, DotnetFieldType.Float)

    override fun double(name: String): DotnetField = addField(name, DotnetFieldType.Double)

    override fun decimal(name: String): DotnetField = addField(name, DotnetFieldType.Decimal)

    override fun bool(name: String): DotnetField = addField(name, DotnetFieldType.Bool)

    override fun guid(name: String): DotnetField = addField(name, DotnetFieldType.Guid)

    override fun dateOnly(name: String): DotnetField = addField(name, DotnetFieldType.DateOnly)

    override fun timeOnly(name: String): DotnetField = addField(name, DotnetFieldType.TimeOnly)

    override fun dateTime(name: String): DotnetField = addField(name, DotnetFieldType.DateTime)

    override fun dateTimeOffset(name: String): DotnetField = addField(name, DotnetFieldType.DateTimeOffset)

    override fun timeSpan(name: String): DotnetField = addField(name, DotnetFieldType.TimeSpan)

    override infix fun String.ref(target: String): DotnetField = addReference(this, target)

    override infix fun String.references(target: String): DotnetField = this ref target

    protected fun buildFields(): List<DotnetField> = fieldsByName.values.toList()

    protected fun addField(name: String, type: DotnetFieldType): DotnetField {
        val fieldName = validateDotnetIdentifier(name, fieldNameLabel)
        require(fieldName !in fieldsByName) { duplicateFieldMessage(fieldName) }

        val field = DotnetField(name = fieldName, type = type)
        fieldsByName[fieldName] = field
        return field
    }

    protected fun addReference(name: String, target: String): DotnetField =
        addField(name, DotnetFieldType.Reference(target))
}
