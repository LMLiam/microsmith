package io.github.lmliam.microsmith.compile.services.dotnet.asp.names

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifact

internal fun contractsNamespace(artifact: DotnetAspServiceArtifact): String =
    "${artifact.id.projectName}.Generated.Contracts"

internal fun controllersNamespace(artifact: DotnetAspServiceArtifact): String =
    "${artifact.id.projectName}.Generated.Controllers"

internal fun hostingNamespace(artifact: DotnetAspServiceArtifact): String =
    "${artifact.id.projectName}.Generated.Hosting"

internal fun controllerRelativePath(typeName: String): String = "Generated/Controllers/$typeName.cs"
