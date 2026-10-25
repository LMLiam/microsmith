package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.model.DotnetAspModelReference
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspHeadersBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspRequestBinding

data class DotnetAspEndpointBindings(
    val path: DotnetAspRequestBinding? = null,
    val query: DotnetAspRequestBinding? = null,
    val headers: DotnetAspHeadersBinding? = null,
    val body: DotnetAspModelReference? = null,
)
