package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

data class DotnetAspRequestField(
    val name: String,
    val type: DotnetFieldType,
    val optional: Boolean = false,
    val defaultValue: Any? = null,
) {
    init {
        DotnetField(name, type)
        require(
            defaultValue == null ||
                defaultValue is String ||
                defaultValue is Char ||
                defaultValue is Number ||
                defaultValue is Boolean,
        ) {
            "ASP.NET request field default for '$name' must be a string, char, number, or boolean."
        }
    }
}
