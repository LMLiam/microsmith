package io.github.lmliam.microsmith.artifact.services.dotnet.asp.service

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.endpoint.DotnetAspEndpointBindingsArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.model.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.model.DotnetAspModelLocality
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.request.DotnetAspHeaderFieldArtifactMapper
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.request.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.request.DotnetAspRequestBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.request.DotnetAspRequestFieldArtifactMapper
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.response.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.response.DotnetAspResponseHeaderArtifactMapper
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspService
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.dotnetAspResultBaseTypeName
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.dotnetAspResultVariantTypeName

internal class DotnetAspServiceArtifactFactory(
    private val service: ResolvedDotnetAspService,
    private val artifactId: DotnetAspServiceArtifactId,
) {
    private val usedTypeNames = linkedSetOf<String>()

    private val sharedModelsByName =
        service.models.values.sortedBy(DotnetModel::name).associate { model ->
            usedTypeNames += model.name
            model.name to
                DotnetAspModelArtifact(
                    typeName = model.name,
                    locality = DotnetAspModelLocality.SHARED,
                    model = model,
                )
        }

    init {
        service.rest.endpoints.forEach { endpoint ->
            usedTypeNames += dotnetAspResultBaseTypeName(endpoint.operationName)
            endpoint.responses.forEach { response ->
                usedTypeNames += dotnetAspResultVariantTypeName(endpoint.operationName, response.statusCode)
            }
        }
    }

    private val contractModels = mutableListOf<DotnetAspModelArtifact>().apply { addAll(sharedModelsByName.values) }

    fun createContribution(): DotnetAspServiceContribution {
        val endpoints = endpointArtifacts()
        return DotnetAspServiceContribution(
            artifactId = artifactId,
            serviceName = service.name,
            targetFrameworkMoniker = service.targetFrameworkMoniker,
            outputRoot = service.outputRoot,
            httpPort = service.ports.http,
            httpsPort = service.ports.https,
            contractModels = contractModels.toList(),
            endpoints = endpoints,
        )
    }

    private fun endpointArtifacts(): List<DotnetAspEndpointArtifact> =
        service.rest.endpoints.map { endpoint ->
            DotnetAspEndpointArtifact(
                method = endpoint.method.name,
                route = endpoint.route,
                operationName = endpoint.operationName,
                bindings =
                    DotnetAspEndpointBindingsArtifact(
                        path = endpoint.bindings.path?.toRequestBindingArtifact(endpoint),
                        query = endpoint.bindings.query?.toRequestBindingArtifact(endpoint),
                        headers = endpoint.bindings.headers?.toHeadersBindingArtifact(endpoint),
                        body = endpoint.bindings.body?.toModelArtifact(endpoint, "body"),
                    ),
                responses = endpoint.responses.map { response -> response.toResponseArtifact(endpoint) },
            )
        }

    private fun ResolvedDotnetAspRequestBinding.toRequestBindingArtifact(
        endpoint: ResolvedDotnetAspEndpoint,
    ): DotnetAspRequestBindingArtifact =
        DotnetAspRequestBindingArtifact(
            typeName = allocateDotnetAspTypeName(usedTypeNames, name, "${endpoint.operationName}$name"),
            name = name,
            fields = DotnetAspRequestFieldArtifactMapper.mapList(fields),
        )

    private fun ResolvedDotnetAspHeadersBinding.toHeadersBindingArtifact(
        endpoint: ResolvedDotnetAspEndpoint,
    ): DotnetAspHeadersBindingArtifact =
        DotnetAspHeadersBindingArtifact(
            typeName = allocateDotnetAspTypeName(usedTypeNames, name, "${endpoint.operationName}$name"),
            name = name,
            headers = DotnetAspHeaderFieldArtifactMapper.mapList(headers),
        )

    private fun ResolvedDotnetAspModel.toModelArtifact(
        endpoint: ResolvedDotnetAspEndpoint,
        originKind: String,
    ): DotnetAspModelArtifact =
        when (locality) {
            ResolvedDotnetAspModelLocality.SHARED ->
                requireNotNull(sharedModelsByName[model.name]) {
                    "Missing shared ASP.NET model artifact for '${model.name}'."
                }

            ResolvedDotnetAspModelLocality.INLINE ->
                DotnetAspModelArtifact(
                        typeName =
                            allocateDotnetAspTypeName(
                                usedTypeNames,
                                model.name,
                                "${endpoint.operationName}${model.name}",
                                "${endpoint.operationName}${originKind.replaceFirstChar(Char::uppercase)}",
                            ),
                        locality = DotnetAspModelLocality.INLINE,
                        model = model,
                    )
                    .also(contractModels::add)
        }

    private fun ResolvedDotnetAspResponse.toResponseArtifact(
        endpoint: ResolvedDotnetAspEndpoint,
    ): DotnetAspResponseArtifact {
        val modelArtifact = model.toModelArtifact(endpoint, "responses.$statusCode")
        return DotnetAspResponseArtifact(
            statusCode = statusCode,
            model = modelArtifact,
            headers = DotnetAspResponseHeaderArtifactMapper.mapList(headers),
        )
    }
}
