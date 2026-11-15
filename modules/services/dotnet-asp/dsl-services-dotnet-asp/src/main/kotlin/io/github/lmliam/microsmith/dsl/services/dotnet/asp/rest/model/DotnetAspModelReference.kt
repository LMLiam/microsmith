package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.model

import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

public sealed interface DotnetAspModelReference {
    public data class Shared(public val target: String) : DotnetAspModelReference {
        init {
            validateDotnetIdentifier(target, "ASP.NET shared model reference")
        }
    }

    public data class Inline(public val model: DotnetModel) : DotnetAspModelReference
}
