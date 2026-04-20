package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.DotnetAspWorkspace
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspService

@ServiceProvider(ArtifactContributor::class)
class DotnetAspArtifactContributor : ArtifactContributor<DotnetAspWorkspace> {
    override val resolvedType = DotnetAspWorkspace::class

    override fun contribute(model: DotnetAspWorkspace): List<ArtifactContribution<*>> =
        model.servicesByName.values.sortedBy {
            it.name
        }.mapIndexed(::toContribution)

    private fun toContribution(index: Int, service: ResolvedDotnetAspService): DotnetAspServiceContribution {
            val httpPort = BASE_HTTP_PORT + (index * PORT_STRIDE)
            val usedTypeNames = linkedSetOf<String>()
            val sharedModelsByName = service.models.values
                .sortedBy(DotnetModel::name)
                .associate { model ->
                    usedTypeNames += model.name
                    model.name to DotnetAspModelArtifact(
                        typeName = model.name,
                        locality = DotnetAspModelLocality.SHARED,
                        model = model,
                        origins = setOf("services.${service.name}.models.${model.name}"),
                    )
                }
            val contractModels = mutableListOf<DotnetAspModelArtifact>()
            contractModels += sharedModelsByName.values

            val endpoints = service.rest.endpoints.map { endpoint ->
                val endpointOrigin = "services.${service.name}.rest.${endpoint.operationName}"
                DotnetAspEndpointArtifact(
                    method = endpoint.method.name,
                    route = endpoint.route,
                    operationName = endpoint.operationName,
                    bindings = DotnetAspEndpointBindingsArtifact(
                        path = endpoint.bindings.path?.toRequestBindingArtifact(
                            serviceName = service.name,
                            endpoint = endpoint,
                            bindingLabel = "path",
                            usedTypeNames = usedTypeNames,
                        ),
                        query = endpoint.bindings.query?.toRequestBindingArtifact(
                            serviceName = service.name,
                            endpoint = endpoint,
                            bindingLabel = "query",
                            usedTypeNames = usedTypeNames,
                        ),
                        headers = endpoint.bindings.headers?.toHeadersBindingArtifact(
                            serviceName = service.name,
                            endpoint = endpoint,
                            usedTypeNames = usedTypeNames,
                        ),
                        body = endpoint.bindings.body?.toModelArtifact(
                            serviceName = service.name,
                            endpoint = endpoint,
                            sharedModelsByName = sharedModelsByName,
                            usedTypeNames = usedTypeNames,
                            contractModels = contractModels,
                            originKind = "body",
                        ),
                    ),
                    responses = endpoint.responses.map { response ->
                        response.toResponseArtifact(
                            serviceName = service.name,
                            endpoint = endpoint,
                            sharedModelsByName = sharedModelsByName,
                            usedTypeNames = usedTypeNames,
                            contractModels = contractModels,
                        )
                    },
                    origins = setOf(endpointOrigin),
                )
            }

            return DotnetAspServiceContribution(
                artifactId = DotnetAspServiceArtifactId(service.solutionName, service.projectName),
                serviceName = service.name,
                targetFrameworkMoniker = service.targetFrameworkMoniker,
                outputRoot = service.outputRoot,
                httpPort = httpPort,
                httpsPort = httpPort + 1,
                contractModels = contractModels.toList(),
                endpoints = endpoints,
            )
        }

    private companion object {
        const val BASE_HTTP_PORT = 5000
        const val PORT_STRIDE = 10
    }

    private fun ResolvedDotnetAspRequestBinding.toRequestBindingArtifact(
        serviceName: String,
        endpoint: ResolvedDotnetAspEndpoint,
        bindingLabel: String,
        usedTypeNames: MutableSet<String>,
    ): DotnetAspRequestBindingArtifact {
        val origin = "services.$serviceName.rest.${endpoint.operationName}.$bindingLabel.$name"
        return DotnetAspRequestBindingArtifact(
            typeName = allocateTypeName(usedTypeNames, name, "${endpoint.operationName}$name"),
            name = name,
            fields = fields.map { field ->
                DotnetAspRequestFieldArtifact(
                    name = field.name,
                    type = field.type,
                    optional = field.optional,
                    defaultValue = field.defaultValue,
                )
            },
            origins = setOf(origin),
        )
    }

    private fun ResolvedDotnetAspHeadersBinding.toHeadersBindingArtifact(
        serviceName: String,
        endpoint: ResolvedDotnetAspEndpoint,
        usedTypeNames: MutableSet<String>,
    ): DotnetAspHeadersBindingArtifact {
        val origin = "services.$serviceName.rest.${endpoint.operationName}.headers.$name"
        return DotnetAspHeadersBindingArtifact(
            typeName = allocateTypeName(usedTypeNames, name, "${endpoint.operationName}$name"),
            name = name,
            headers = headers.map { header ->
                DotnetAspHeaderFieldArtifact(name = header.name, headerName = header.headerName)
            },
            origins = setOf(origin),
        )
    }

    private fun ResolvedDotnetAspModel.toModelArtifact(
        serviceName: String,
        endpoint: ResolvedDotnetAspEndpoint,
        sharedModelsByName: Map<String, DotnetAspModelArtifact>,
        usedTypeNames: MutableSet<String>,
        contractModels: MutableList<DotnetAspModelArtifact>,
        originKind: String,
    ): DotnetAspModelArtifact = when (locality) {
        ResolvedDotnetAspModelLocality.SHARED -> requireNotNull(sharedModelsByName[model.name]) {
            "Missing shared ASP.NET model artifact for '${model.name}'."
        }

        ResolvedDotnetAspModelLocality.INLINE -> DotnetAspModelArtifact(
            typeName = allocateTypeName(
                usedTypeNames,
                model.name,
                "${endpoint.operationName}${model.name}",
                "${endpoint.operationName}${originKind.replaceFirstChar(Char::uppercase)}",
            ),
            locality = DotnetAspModelLocality.INLINE,
            model = model,
            origins = setOf("services.$serviceName.rest.${endpoint.operationName}.$originKind.${model.name}"),
        ).also(contractModels::add)
    }

    private fun ResolvedDotnetAspResponse.toResponseArtifact(
        serviceName: String,
        endpoint: ResolvedDotnetAspEndpoint,
        sharedModelsByName: Map<String, DotnetAspModelArtifact>,
        usedTypeNames: MutableSet<String>,
        contractModels: MutableList<DotnetAspModelArtifact>,
    ): DotnetAspResponseArtifact {
        val responseOrigin = "services.$serviceName.rest.${endpoint.operationName}.responses.$statusCode"
        val modelArtifact = model.toModelArtifact(
            serviceName = serviceName,
            endpoint = endpoint,
            sharedModelsByName = sharedModelsByName,
            usedTypeNames = usedTypeNames,
            contractModels = contractModels,
            originKind = "responses.$statusCode",
        )
        return DotnetAspResponseArtifact(
            statusCode = statusCode,
            model = modelArtifact,
            headers = headers.map { header -> DotnetAspResponseHeaderArtifact(header.name) },
            origins = setOf(responseOrigin) + modelArtifact.origins,
        )
    }

    private fun allocateTypeName(usedTypeNames: MutableSet<String>, vararg candidates: String): String {
        candidates
            .map(String::trim)
            .filter(String::isNotBlank)
            .firstOrNull { candidate -> usedTypeNames.add(candidate) }
            ?.let { return it }

        val fallbackBase = candidates.firstOrNull(String::isNotBlank)?.trim().orEmpty()
        var suffix = 2
        while (true) {
            val candidate = "$fallbackBase$suffix"
            if (usedTypeNames.add(candidate)) {
                return candidate
            }
            suffix += 1
        }
    }
}
