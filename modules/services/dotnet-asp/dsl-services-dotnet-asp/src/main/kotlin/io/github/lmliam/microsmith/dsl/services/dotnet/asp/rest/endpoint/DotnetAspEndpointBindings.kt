package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.model.DotnetAspModelReference
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspHeadersBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspRequestBinding

public data class DotnetAspEndpointBindings(
    public val path: DotnetAspRequestBinding? = null,
    public val query: DotnetAspRequestBinding? = null,
    public val headers: DotnetAspHeadersBinding? = null,
    public val body: DotnetAspModelReference? = null,
)
