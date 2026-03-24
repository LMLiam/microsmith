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

    override fun string(name: String) = addField(name, DotnetFieldType.StringType)

    override fun char(name: String) = addField(name, DotnetFieldType.CharType)

    override fun byte(name: String) = addField(name, DotnetFieldType.ByteType)

    override fun short(name: String) = addField(name, DotnetFieldType.ShortType)

    override fun int(name: String) = addField(name, DotnetFieldType.IntType)

    override fun long(name: String) = addField(name, DotnetFieldType.LongType)

    override fun float(name: String) = addField(name, DotnetFieldType.FloatType)

    override fun double(name: String) = addField(name, DotnetFieldType.DoubleType)

    override fun decimal(name: String) = addField(name, DotnetFieldType.DecimalType)

    override fun bool(name: String) = addField(name, DotnetFieldType.BoolType)

    override fun guid(name: String) = addField(name, DotnetFieldType.GuidType)

    override fun dateOnly(name: String) = addField(name, DotnetFieldType.DateOnlyType)

    override fun timeOnly(name: String) = addField(name, DotnetFieldType.TimeOnlyType)

    override fun dateTime(name: String) = addField(name, DotnetFieldType.DateTimeType)

    override fun dateTimeOffset(name: String) = addField(name, DotnetFieldType.DateTimeOffsetType)

    override fun timeSpan(name: String) = addField(name, DotnetFieldType.TimeSpanType)

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
