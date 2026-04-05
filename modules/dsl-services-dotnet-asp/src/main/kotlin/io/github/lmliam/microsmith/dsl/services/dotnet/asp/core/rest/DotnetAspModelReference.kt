package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel

sealed interface DotnetAspModelReference {
    data class Shared(val target: String) : DotnetAspModelReference {
        init {
            DotnetField(target, DotnetFieldType.String)
        }
    }

    data class Inline(val model: DotnetModel) : DotnetAspModelReference
}
