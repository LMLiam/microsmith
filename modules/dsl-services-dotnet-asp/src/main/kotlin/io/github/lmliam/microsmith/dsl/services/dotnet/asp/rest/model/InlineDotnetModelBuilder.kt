package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.model

import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldSetBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModelScope
import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

internal class InlineDotnetModelBuilder(private val modelName: String) :
    DotnetFieldSetBuilder(
        fieldNameLabel = "ASP.NET inline model field name",
        duplicateFieldMessage = { fieldName ->
            "Duplicate ASP.NET inline model field '$fieldName' in model '$modelName'."
        },
    ),
    DotnetModelScope {
    fun build() = DotnetModel(
        name = validateDotnetIdentifier(modelName, "ASP.NET inline model name"),
        fields = buildFields(),
    )
}
