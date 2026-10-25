package io.github.lmliam.microsmith.dsl.services.dotnet.model

import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

internal class DotnetModelBuilder(private val name: String) : DotnetFieldSetBuilder(), DotnetModelScope {

    fun build(): DotnetModel =
        DotnetModel(
            name = validateDotnetIdentifier(name, "Model name"),
            fields = buildFields(),
        )
}
