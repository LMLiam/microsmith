package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse

internal const val MICROSMITH_CONTROLLER_BASE_TYPE_NAME: String = "MicrosmithControllerBase"
internal const val RESULT_BODY_PROPERTY_NAME: String = "Body"

internal fun contractsNamespace(artifact: DotnetAspServiceArtifact) = "${artifact.id.projectName}.Generated.Contracts"

internal fun controllersNamespace(artifact: DotnetAspServiceArtifact) =
    "${artifact.id.projectName}.Generated.Controllers"

internal fun hostingNamespace(artifact: DotnetAspServiceArtifact): String =
    "${artifact.id.projectName}.Generated.Hosting"

internal fun controllerPrefix(artifact: DotnetAspServiceArtifact) = dotnetAspTypeName(artifact.id.projectName)

internal fun controllerBaseTypeName(artifact: DotnetAspServiceArtifact): String =
    "${controllerPrefix(artifact)}ControllerBase"

internal fun microsmithControllerBaseRelativePath(): String =
    "Generated/Controllers/$MICROSMITH_CONTROLLER_BASE_TYPE_NAME.cs"

internal fun controllerBaseRelativePath(artifact: DotnetAspServiceArtifact): String =
    "Generated/Controllers/${controllerBaseTypeName(artifact)}.cs"

internal fun resultBaseTypeName(endpoint: ResolvedDotnetAspEndpoint) = "${endpoint.operationName}Result"

internal fun resultVariantTypeName(endpoint: ResolvedDotnetAspEndpoint, response: ResolvedDotnetAspResponse) =
    endpoint.operationName + dotnetAspStatusName(response.statusCode)

internal fun inlineBodyTypeName(endpoint: ResolvedDotnetAspEndpoint) =
    endpoint.operationName + requireNotNull(endpoint.bindings.body).model.name

internal fun inlineResponseTypeName(endpoint: ResolvedDotnetAspEndpoint, response: ResolvedDotnetAspResponse) =
    endpoint.operationName + dotnetAspStatusName(response.statusCode) + response.model.model.name

internal fun dotnetAspRouteLiteral(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
