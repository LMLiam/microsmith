package io.github.lmliam.microsmith.dsl.services.dotnet.core.model

import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

internal class DotnetModelBuilder(
    private val name: String,
) : DotnetFieldSetBuilder(), DotnetModelScope {

    fun build(): DotnetModel {
        return DotnetModel(
            name = validateDotnetIdentifier(name, "Model name"),
            fields = buildFields(),
        )
    }

    override fun fieldNameLabel(): String = "Field name"

    override fun duplicateFieldMessage(fieldName: String): String =
        "Duplicate .NET field registration for '$fieldName'."
}
