package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.AbstractDotnetModelBuilder

internal class InlineDotnetModelBuilder(
    private val modelName: String,
) : AbstractDotnetModelBuilder() {
    private val fieldsByName = linkedMapOf<String, DotnetField>()

    fun build() = DotnetModel(name = modelName, fields = fieldsByName.values.toList())

    override fun addField(name: String, type: DotnetFieldType): DotnetField {
        val fieldName = name
        require(fieldName !in fieldsByName) {
            "Duplicate ASP.NET inline model field '$fieldName' in model '$modelName'."
        }

        val field = DotnetField(fieldName, type)
        fieldsByName[fieldName] = field
        return field
    }
}
