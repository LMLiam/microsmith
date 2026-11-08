package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.resolve.ResolvedModel

data class DotnetAspWorkspace(val servicesByName: Map<String, ResolvedDotnetAspService>) : ResolvedModel
