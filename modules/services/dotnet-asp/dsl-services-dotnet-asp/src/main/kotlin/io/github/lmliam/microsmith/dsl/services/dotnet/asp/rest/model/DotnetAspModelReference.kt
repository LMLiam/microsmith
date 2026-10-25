package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.model

import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

sealed interface DotnetAspModelReference {
    data class Shared(val target: String) : DotnetAspModelReference {
        init {
            validateDotnetIdentifier(target, "ASP.NET shared model reference")
        }
    }

    data class Inline(val model: DotnetModel) : DotnetAspModelReference
}
