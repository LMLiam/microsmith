package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelLocality
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact

internal fun sharedContractModelOriginsFor(
    artifact: DotnetAspServiceArtifact,
    serviceOrigin: Set<String>,
): Set<String> = artifact.contractModels
    .distinctBy(DotnetAspModelArtifact::typeName)
    .filter { it.locality == DotnetAspModelLocality.SHARED }
    .flatMapTo(linkedSetOf()) { it.origins } + serviceOrigin

internal fun requestModelOriginsFor(artifact: DotnetAspServiceArtifact, serviceOrigin: Set<String>): Set<String> =
    serviceOrigin +
        collectRequestBindings(artifact).flatMapTo(linkedSetOf()) { it.origins } +
        collectHeaderBindings(artifact).flatMapTo(linkedSetOf()) { it.origins } +
        artifact.endpoints.mapNotNull { endpoint ->
            endpoint.bindings.body
                ?.takeIf { it.locality == DotnetAspModelLocality.INLINE }
                ?.origins
        }.flatten()

internal fun responseModelOriginsFor(artifact: DotnetAspServiceArtifact, serviceOrigin: Set<String>): Set<String> =
    serviceOrigin +
        artifact.endpoints.flatMapTo(linkedSetOf()) { endpoint ->
            endpoint.responses.flatMap { response ->
                response.origins + response.model.origins
            }
        }

internal fun controllerOriginsFor(artifact: DotnetAspServiceArtifact, serviceOrigin: Set<String>): Set<String> =
    serviceOrigin +
        artifact.endpoints.flatMapTo(linkedSetOf()) { endpoint ->
            endpoint.origins +
                endpoint.responses.flatMapTo(linkedSetOf()) { it.origins } +
                listOfNotNull(
                    endpoint.bindings.path?.origins,
                    endpoint.bindings.query?.origins,
                    endpoint.bindings.headers?.origins,
                    endpoint.bindings.body?.origins,
                ).flatten()
        }
