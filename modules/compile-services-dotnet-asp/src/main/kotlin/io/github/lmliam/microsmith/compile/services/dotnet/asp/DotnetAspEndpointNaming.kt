package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspHttpMethod
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse

internal fun contractsNamespace(artifact: DotnetAspServiceArtifact) = "${artifact.id.projectName}.Generated.Contracts"

internal fun controllersNamespace(artifact: DotnetAspServiceArtifact) =
    "${artifact.id.projectName}.Generated.Controllers"

internal fun hostingNamespace(artifact: DotnetAspServiceArtifact): String =
    "${artifact.id.projectName}.Generated.Hosting"

internal fun controllerPrefix(artifact: DotnetAspServiceArtifact) = dotnetAspTypeName(artifact.id.projectName)

internal fun httpMethodAttribute(method: DotnetAspHttpMethod): String = when (method) {
    DotnetAspHttpMethod.GET -> "HttpGet"
    DotnetAspHttpMethod.POST -> "HttpPost"
    DotnetAspHttpMethod.PUT -> "HttpPut"
    DotnetAspHttpMethod.PATCH -> "HttpPatch"
    DotnetAspHttpMethod.DELETE -> "HttpDelete"
}

internal fun resultBaseTypeName(endpoint: ResolvedDotnetAspEndpoint) = "${endpoint.operationName}Result"

internal fun resultVariantTypeName(endpoint: ResolvedDotnetAspEndpoint, response: ResolvedDotnetAspResponse) =
    endpoint.operationName + dotnetAspStatusName(response.statusCode)

internal fun inlineBodyTypeName(endpoint: ResolvedDotnetAspEndpoint) =
    endpoint.operationName + requireNotNull(endpoint.bindings.body).model.name

internal fun inlineResponseTypeName(endpoint: ResolvedDotnetAspEndpoint, response: ResolvedDotnetAspResponse) =
    endpoint.operationName + dotnetAspStatusName(response.statusCode) + response.model.model.name

internal fun dotnetAspRouteLiteral(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
