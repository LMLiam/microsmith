package io.github.lmliam.microsmith.compile.services.dotnet.asp.names

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.response.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.MICROSMITH_CONTROLLER_BASE_TYPE_NAME
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.dotnetAspControllerBaseTypeName
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.dotnetAspHeaderPropertyName as resolvedDotnetAspHeaderPropertyName
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.dotnetAspResultBaseTypeName
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.dotnetAspResultVariantTypeName
import java.util.Locale

internal fun contractsNamespace(artifact: DotnetAspServiceArtifact): String =
    "${artifact.id.projectName}.Generated.Contracts"

internal fun controllersNamespace(artifact: DotnetAspServiceArtifact): String =
    "${artifact.id.projectName}.Generated.Controllers"

internal fun hostingNamespace(artifact: DotnetAspServiceArtifact): String =
    "${artifact.id.projectName}.Generated.Hosting"

internal fun controllerBaseTypeName(artifact: DotnetAspServiceArtifact): String =
    dotnetAspControllerBaseTypeName(artifact.id.projectName)

internal fun microsmithControllerBaseRelativePath(): String =
    "Generated/Controllers/$MICROSMITH_CONTROLLER_BASE_TYPE_NAME.cs"

internal fun controllerBaseRelativePath(artifact: DotnetAspServiceArtifact): String =
    "Generated/Controllers/${controllerBaseTypeName(artifact)}.cs"

internal fun resultBaseTypeName(endpoint: DotnetAspEndpointArtifact): String =
    dotnetAspResultBaseTypeName(endpoint.operationName)

internal fun resultVariantTypeName(endpoint: DotnetAspEndpointArtifact, response: DotnetAspResponseArtifact): String =
    dotnetAspResultVariantTypeName(endpoint.operationName, response.statusCode)

internal fun dotnetAspPascalIdentifier(identifier: String): String =
    when {
        identifier.startsWith("@") && identifier.length > 1 ->
            "@${identifier.substring(1).replaceFirstChar { firstChar ->
            firstChar.titlecase(Locale.ROOT)
        }}"

        else -> identifier.replaceFirstChar { firstChar -> firstChar.titlecase(Locale.ROOT) }
    }

internal fun dotnetAspHeaderPropertyName(headerName: String): String =
    checkNotNull(resolvedDotnetAspHeaderPropertyName(headerName)) {
        "Response header '$headerName' has no generated property name; resolution should have reported it."
    }
