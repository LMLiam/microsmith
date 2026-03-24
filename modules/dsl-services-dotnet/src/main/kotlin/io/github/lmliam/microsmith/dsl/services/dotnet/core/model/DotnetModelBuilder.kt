package io.github.lmliam.microsmith.dsl.services.dotnet.core.model

import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

internal class DotnetModelBuilder(
    private val name: String,
) : DotnetModelScope {
    private val fieldsByName = linkedMapOf<String, DotnetField>()

    fun build(): DotnetModel {
        return DotnetModel(
            name = validateDotnetIdentifier(name, "Model name"),
            fields = fieldsByName.values.toList(),
        )
    }

    override fun string(name: String) = addField(name, DotnetFieldType.String)

    override fun char(name: String) = addField(name, DotnetFieldType.Char)

    override fun byte(name: String) = addField(name, DotnetFieldType.Byte)

    override fun sbyte(name: String) = addField(name, DotnetFieldType.SByte)

    override fun short(name: String) = addField(name, DotnetFieldType.Short)

    override fun ushort(name: String) = addField(name, DotnetFieldType.UShort)

    override fun int(name: String) = addField(name, DotnetFieldType.Int)

    override fun uint(name: String) = addField(name, DotnetFieldType.UInt)

    override fun long(name: String) = addField(name, DotnetFieldType.Long)

    override fun ulong(name: String) = addField(name, DotnetFieldType.ULong)

    override fun nint(name: String) = addField(name, DotnetFieldType.NInt)

    override fun nuint(name: String) = addField(name, DotnetFieldType.NUInt)

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

    override infix fun String.ref(target: String) = addField(
        name = this,
        type = DotnetFieldType.Reference(validateDotnetIdentifier(target, "Reference target")),
    )

    private fun addField(name: String, type: DotnetFieldType): DotnetField {
        val fieldName = validateDotnetIdentifier(name, "Field name")
        require(fieldName !in fieldsByName) {
            "Duplicate .NET field registration for '$fieldName'."
        }

        val field = DotnetField(name = fieldName, type = type)
        fieldsByName[fieldName] = field
        return field
    }
}
