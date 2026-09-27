package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.response

data class DotnetAspResponseHeader(val name: String) {
    init {
        require(name.isNotBlank()) {
            "ASP.NET response header name cannot be blank."
        }
    }
}
