package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

data class DotnetAspEndpointBindings(
    val path: DotnetAspRequestBinding? = null,
    val query: DotnetAspRequestBinding? = null,
    val headers: DotnetAspHeadersBinding? = null,
    val body: DotnetAspModelReference? = null,
)
