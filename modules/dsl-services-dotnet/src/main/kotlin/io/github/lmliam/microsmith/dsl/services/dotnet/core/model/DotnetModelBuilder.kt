package io.github.lmliam.microsmith.dsl.services.dotnet.core.model

import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

internal class DotnetModelBuilder(
    private val name: String,
) : AbstractDotnetModelBuilder() {
    private val fieldsByName = linkedMapOf<String, DotnetField>()

    fun build(): DotnetModel {
        return DotnetModel(
            name = validateDotnetIdentifier(name, "Model name"),
            fields = fieldsByName.values.toList(),
        )
    }

    override fun addField(name: String, type: DotnetFieldType): DotnetField {
        val fieldName = validateDotnetIdentifier(name, "Field name")
        require(fieldName !in fieldsByName) {
            "Duplicate .NET field registration for '$fieldName'."
        }

        val field = DotnetField(name = fieldName, type = type)
        fieldsByName[fieldName] = field
        return field
    }

    override fun addReference(name: String, target: String) = addField(
        name = name,
        type = DotnetFieldType.Reference(validateDotnetIdentifier(target, "Reference target")),
    )
}
