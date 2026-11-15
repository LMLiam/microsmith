package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import io.github.lmliam.microsmith.resolve.services.ResolvedServicesModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspService

public data class DotnetAspWorkspace(public val servicesByName: Map<String, ResolvedDotnetAspService>) :
    ResolvedServicesModel
