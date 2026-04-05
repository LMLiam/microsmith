package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

data class DotnetAspResponseHeader(
    val name: String,
) {
    init {
        require(name.isNotBlank()) {
            "ASP.NET response header name cannot be blank."
        }
    }
}
