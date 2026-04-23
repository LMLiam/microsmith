package io.github.lmliam.microsmith.artifact.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspDefaultValue
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspService

internal class DotnetAspServiceArtifactFactory(
    private val service: ResolvedDotnetAspService,
    private val artifactId: DotnetAspServiceArtifactId,
    private val ports: DotnetAspAllocatedPorts,
) {
    private val usedTypeNames = linkedSetOf<String>()

    private val sharedModelsByName = service.models.values
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

    private val contractModels = mutableListOf<DotnetAspModelArtifact>().apply {
        addAll(sharedModelsByName.values)
    }

    fun createContribution(): DotnetAspServiceContribution {
        val endpoints = endpointArtifacts()
        return DotnetAspServiceContribution(
            artifactId = artifactId,
            serviceName = service.name,
            targetFrameworkMoniker = service.targetFrameworkMoniker,
            outputRoot = service.outputRoot,
            httpPort = ports.http,
            httpsPort = ports.https,
            contractModels = contractModels.toList(),
            endpoints = endpoints,
        )
    }

    private fun endpointArtifacts(): List<DotnetAspEndpointArtifact> = service.rest.endpoints.map { endpoint ->
        DotnetAspEndpointArtifact(
            method = endpoint.method.name,
            route = endpoint.route,
            operationName = endpoint.operationName,
            bindings = DotnetAspEndpointBindingsArtifact(
                path = endpoint.bindings.path?.toRequestBindingArtifact(endpoint, "path"),
                query = endpoint.bindings.query?.toRequestBindingArtifact(endpoint, "query"),
                headers = endpoint.bindings.headers?.toHeadersBindingArtifact(endpoint),
                body = endpoint.bindings.body?.toModelArtifact(endpoint, "body"),
            ),
            responses = endpoint.responses.map { response -> response.toResponseArtifact(endpoint) },
            origins = setOf("services.${service.name}.rest.${endpoint.operationName}"),
        )
    }

    private fun ResolvedDotnetAspRequestBinding.toRequestBindingArtifact(
        endpoint: ResolvedDotnetAspEndpoint,
        bindingLabel: String,
    ): DotnetAspRequestBindingArtifact = DotnetAspRequestBindingArtifact(
        typeName = allocateDotnetAspTypeName(usedTypeNames, name, "${endpoint.operationName}$name"),
        name = name,
        fields = fields.map { field ->
            DotnetAspRequestFieldArtifact(
                name = field.name,
                type = field.type,
                optional = field.optional,
                defaultValue = field.defaultValue?.unwrapDotnetAspDefaultValue(),
            )
        },
        origins = setOf("services.${service.name}.rest.${endpoint.operationName}.$bindingLabel.$name"),
    )

    private fun ResolvedDotnetAspHeadersBinding.toHeadersBindingArtifact(
        endpoint: ResolvedDotnetAspEndpoint,
    ): DotnetAspHeadersBindingArtifact = DotnetAspHeadersBindingArtifact(
        typeName = allocateDotnetAspTypeName(usedTypeNames, name, "${endpoint.operationName}$name"),
        name = name,
        headers = headers.map { header -> DotnetAspHeaderFieldArtifact(header.name, header.headerName) },
        origins = setOf("services.${service.name}.rest.${endpoint.operationName}.headers.$name"),
    )

    private fun ResolvedDotnetAspModel.toModelArtifact(
        endpoint: ResolvedDotnetAspEndpoint,
        originKind: String,
    ): DotnetAspModelArtifact = when (locality) {
        ResolvedDotnetAspModelLocality.SHARED -> requireNotNull(sharedModelsByName[model.name]) {
            "Missing shared ASP.NET model artifact for '${model.name}'."
        }

        ResolvedDotnetAspModelLocality.INLINE -> DotnetAspModelArtifact(
            typeName = allocateDotnetAspTypeName(
                usedTypeNames,
                model.name,
                "${endpoint.operationName}${model.name}",
                "${endpoint.operationName}${originKind.replaceFirstChar(Char::uppercase)}",
            ),
            locality = DotnetAspModelLocality.INLINE,
            model = model,
            origins = setOf("services.${service.name}.rest.${endpoint.operationName}.$originKind.${model.name}"),
        ).also(contractModels::add)
    }

    private fun ResolvedDotnetAspResponse.toResponseArtifact(
        endpoint: ResolvedDotnetAspEndpoint,
    ): DotnetAspResponseArtifact {
        val modelArtifact = model.toModelArtifact(endpoint, "responses.$statusCode")
        return DotnetAspResponseArtifact(
            statusCode = statusCode,
            model = modelArtifact,
            headers = headers.map { header -> DotnetAspResponseHeaderArtifact(header.name) },
            origins = setOf("services.${service.name}.rest.${endpoint.operationName}.responses.$statusCode") +
                modelArtifact.origins,
        )
    }
}

private fun allocateDotnetAspTypeName(usedTypeNames: MutableSet<String>, vararg candidates: String): String {
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

private fun DotnetAspDefaultValue.unwrapDotnetAspDefaultValue(): Any = when (this) {
    is DotnetAspDefaultValue.StringValue -> value
    is DotnetAspDefaultValue.CharValue -> value
    is DotnetAspDefaultValue.NumericValue -> value
    is DotnetAspDefaultValue.BooleanValue -> value
    is DotnetAspDefaultValue.UuidValue -> value
    is DotnetAspDefaultValue.LocalDateValue -> value
    is DotnetAspDefaultValue.LocalTimeValue -> value
    is DotnetAspDefaultValue.LocalDateTimeValue -> value
    is DotnetAspDefaultValue.InstantValue -> value
    is DotnetAspDefaultValue.OffsetDateTimeValue -> value
    is DotnetAspDefaultValue.DurationValue -> value
}
