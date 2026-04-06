package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model.DotnetAspModelReference
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspHeadersBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspRequestBinding

data class DotnetAspEndpointBindings(
    val path: DotnetAspRequestBinding? = null,
    val query: DotnetAspRequestBinding? = null,
    val headers: DotnetAspHeadersBinding? = null,
    val body: DotnetAspModelReference? = null,
)
