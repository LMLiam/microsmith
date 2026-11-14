package io.github.lmliam.microsmith.artifact.services.dotnet.asp.request

import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeaderField
import tech.mappie.api.ObjectMappie

internal object DotnetAspHeaderFieldArtifactMapper :
    ObjectMappie<ResolvedDotnetAspHeaderField, DotnetAspHeaderFieldArtifact>()
