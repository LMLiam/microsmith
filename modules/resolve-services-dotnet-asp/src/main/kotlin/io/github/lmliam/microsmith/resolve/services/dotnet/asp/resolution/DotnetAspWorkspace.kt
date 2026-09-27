package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution
import io.github.lmliam.microsmith.resolve.ResolvedModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspService

data class DotnetAspWorkspace(val servicesByName: Map<String, ResolvedDotnetAspService>) : ResolvedModel
