package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding

internal fun validateEndpointGenerationInputs(artifact: DotnetAspServiceArtifact) {
    validateRequestBindings(artifact)
    validateResponseHeaderNames(artifact)
    validateGeneratedContractTypeNames(artifact)
    validateGeneratedControllerTypeNames(artifact)
}

private fun validateRequestBindings(artifact: DotnetAspServiceArtifact) {
    artifact.rest.endpoints.forEach { endpoint ->
        listOfNotNull(endpoint.bindings.path, endpoint.bindings.query).forEach { binding ->
            binding.fields.forEach { field ->
                val referenceTarget = (field.type as? DotnetFieldType.Reference)?.target
                require(referenceTarget == null) {
                    "ASP.NET request binding '${binding.name}' in operation " +
                        "'${endpoint.operationName}' cannot reference shared model " +
                        "'$referenceTarget'. " +
                        "Transport bindings must declare scalar fields."
                }
            }
        }
    }
}

private fun validateResponseHeaderNames(artifact: DotnetAspServiceArtifact) {
    artifact.rest.endpoints.forEach { endpoint ->
        endpoint.responses.forEach { response ->
            val headerPropertyNames = response.headers.map { header ->
                val generatedName = dotnetAspHeaderPropertyName(header.name)
                require(generatedName != RESULT_BODY_PROPERTY_NAME) {
                    "ASP.NET response ${response.statusCode} in operation " +
                        "'${endpoint.operationName}' declares header '${header.name}', " +
                        "which collides with the generated result body property " +
                        "'$RESULT_BODY_PROPERTY_NAME'."
                }
                generatedName
            }
            val collisions = response.headers
                .zip(headerPropertyNames)
                .groupBy({ (_, generatedName) -> generatedName }, { (header, _) -> header })
                .filterValues { it.size > 1 }
                .keys
                .sorted()
            require(collisions.isEmpty()) {
                "ASP.NET response ${response.statusCode} in operation " +
                    "'${endpoint.operationName}' declares headers with colliding " +
                    "generated property names: ${collisions.joinToString(", ")}."
            }
        }
    }
}

private fun validateGeneratedControllerTypeNames(artifact: DotnetAspServiceArtifact) {
    require(controllerBaseTypeName(artifact) != MICROSMITH_CONTROLLER_BASE_TYPE_NAME) {
        "ASP.NET service '${artifact.serviceName}' project '${artifact.id.projectName}' " +
            "generates controller base type '${controllerBaseTypeName(artifact)}', " +
            "which collides with shared generated controller base type " +
            "'$MICROSMITH_CONTROLLER_BASE_TYPE_NAME'."
    }
}

private fun validateGeneratedContractTypeNames(artifact: DotnetAspServiceArtifact) {
    val contractOwners = linkedMapOf<String, MutableList<String>>()

    fun register(typeName: String, owner: String) {
        contractOwners.getOrPut(typeName) { mutableListOf() }.add(owner)
    }

    artifact.models.keys.forEach { register(it, "shared model '$it'") }
    collectRequestBindings(artifact).forEach { register(it.name, "request binding '${it.name}'") }
    collectHeaderBindings(artifact).forEach { register(it.name, "headers binding '${it.name}'") }
    artifact.rest.endpoints.forEach { endpoint ->
        register(
            resultBaseTypeName(endpoint),
            "result base for operation '${endpoint.operationName}'",
        )
        endpoint.bindings.body
            ?.takeIf { it.locality == ResolvedDotnetAspModelLocality.INLINE }
            ?.let {
                register(
                    inlineBodyTypeName(endpoint),
                    "inline body model for operation '${endpoint.operationName}'",
                )
            }
        endpoint.responses.forEach { response ->
            register(
                resultVariantTypeName(endpoint, response),
                "response result for operation '${endpoint.operationName}' " +
                    "status ${response.statusCode}",
            )
            if (response.model.locality == ResolvedDotnetAspModelLocality.INLINE) {
                register(
                    inlineResponseTypeName(endpoint, response),
                    "inline response model for operation '${endpoint.operationName}' " +
                        "status ${response.statusCode}",
                )
            }
        }
    }

    val collisions = contractOwners
        .filterValues { it.size > 1 }
        .entries
        .sortedBy { it.key }
    require(collisions.isEmpty()) {
        "ASP.NET service '${artifact.serviceName}' produces colliding generated contract types: " +
            collisions.joinToString("; ") { (typeName, owners) ->
                "$typeName from ${owners.sorted().joinToString(", ")}"
            } + "."
    }
}

internal fun collectRequestBindings(artifact: DotnetAspServiceArtifact): List<ResolvedDotnetAspRequestBinding> =
    artifact
        .rest
        .endpoints
        .flatMap { endpoint ->
            listOfNotNull(endpoint.bindings.path, endpoint.bindings.query)
        }.groupBy(ResolvedDotnetAspRequestBinding::name)
        .map { (name, bindings) ->
            val first = bindings.first()
            require(bindings.all { it == first }) {
                "ASP.NET service '${artifact.serviceName}' declares conflicting " +
                    "request binding shapes for '$name'."
            }
            first
        }.sortedBy(ResolvedDotnetAspRequestBinding::name)

internal fun collectHeaderBindings(artifact: DotnetAspServiceArtifact): List<ResolvedDotnetAspHeadersBinding> = artifact
    .rest
    .endpoints
    .mapNotNull { it.bindings.headers }
    .groupBy(ResolvedDotnetAspHeadersBinding::name)
    .map { (name, bindings) ->
        val first = bindings.first()
        require(bindings.all { it == first }) {
            "ASP.NET service '${artifact.serviceName}' declares conflicting " +
                "headers binding shapes for '$name'."
        }
        first
    }.sortedBy(ResolvedDotnetAspHeadersBinding::name)
