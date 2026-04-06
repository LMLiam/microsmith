package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

sealed interface DotnetAspModelReference {
    data class Shared(val target: String) : DotnetAspModelReference {
        init {
            validateDotnetIdentifier(target, "ASP.NET shared model reference")
        }
    }

    data class Inline(val model: DotnetModel) : DotnetAspModelReference
}
