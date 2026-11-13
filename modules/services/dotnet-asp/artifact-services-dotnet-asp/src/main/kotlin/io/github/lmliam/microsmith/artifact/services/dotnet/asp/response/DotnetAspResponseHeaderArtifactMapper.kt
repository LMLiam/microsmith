package io.github.lmliam.microsmith.artifact.services.dotnet.asp.response

import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponseHeader
import tech.mappie.api.ObjectMappie

internal object DotnetAspResponseHeaderArtifactMapper :
    ObjectMappie<ResolvedDotnetAspResponseHeader, DotnetAspResponseHeaderArtifact>()
