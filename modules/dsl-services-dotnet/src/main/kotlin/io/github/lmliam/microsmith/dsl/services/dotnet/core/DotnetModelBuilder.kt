package io.github.lmliam.microsmith.dsl.services.dotnet.core

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

    override fun int(name: String) = addField(name, DotnetFieldType.IntType)

    override fun long(name: String) = addField(name, DotnetFieldType.LongType)

    override fun bool(name: String) = addField(name, DotnetFieldType.BoolType)

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
