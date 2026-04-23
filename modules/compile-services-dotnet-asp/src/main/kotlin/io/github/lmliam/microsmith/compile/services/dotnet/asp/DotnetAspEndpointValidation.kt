package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

internal fun validateEndpointGenerationInputs(artifact: DotnetAspServiceArtifact) {
    validateRequestBindings(artifact)
    validateNoContentResponses(artifact)
    validateResponseHeaderNames(artifact)
    validateGeneratedContractTypeNames(artifact)
    validateGeneratedControllerTypeNames(artifact)
}

private fun validateRequestBindings(artifact: DotnetAspServiceArtifact) {
    artifact.endpoints.forEach { endpoint ->
        listOfNotNull(endpoint.bindings.path, endpoint.bindings.query).forEach { binding ->
            binding.fields.forEach { field ->
                val referenceTarget = (field.type as? DotnetFieldType.Reference)?.target
                require(referenceTarget == null) {
                    "ASP.NET request binding '${binding.typeName}' in operation " +
                        "'${endpoint.operationName}' cannot reference shared model " +
                        "'$referenceTarget'. " +
                        "Transport bindings must declare scalar fields."
                }
            }
        }
    }
}

private fun validateResponseHeaderNames(artifact: DotnetAspServiceArtifact) {
    artifact.endpoints.forEach { endpoint ->
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

private fun validateNoContentResponses(artifact: DotnetAspServiceArtifact) {
    artifact.endpoints.forEach { endpoint ->
        endpoint.responses
            .filter { response -> response.statusCode == HTTP_NO_CONTENT_STATUS_CODE }
            .forEach { response ->
                require(response.model.model.fields.isEmpty()) {
                    "ASP.NET response ${response.statusCode} in operation " +
                        "'${endpoint.operationName}' cannot declare response body fields " +
                        "for model '${response.model.typeName}'. " +
                        "HTTP 204 responses are emitted without a response body."
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

    artifact.contractModels
        .distinctBy { it.typeName }
        .forEach { model ->
            register(model.typeName, "generated contract model '${model.typeName}'")
        }
    collectRequestBindings(artifact).forEach { register(it.typeName, "request binding '${it.typeName}'") }
    collectHeaderBindings(artifact).forEach { register(it.typeName, "headers binding '${it.typeName}'") }
    artifact.endpoints.forEach { endpoint ->
        register(
            resultBaseTypeName(endpoint),
            "result base for operation '${endpoint.operationName}'",
        )
        endpoint.responses.forEach { response ->
            register(
                resultVariantTypeName(endpoint, response),
                "response result for operation '${endpoint.operationName}' " +
                    "status ${response.statusCode}",
            )
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

internal fun collectRequestBindings(artifact: DotnetAspServiceArtifact): List<DotnetAspRequestBindingArtifact> =
    artifact
        .endpoints
        .flatMap { endpoint ->
            listOfNotNull(endpoint.bindings.path, endpoint.bindings.query)
        }.groupBy(DotnetAspRequestBindingArtifact::typeName)
        .map { (typeName, bindings) ->
            val first = bindings.first()
            require(bindings.all { it == first }) {
                "ASP.NET service '${artifact.serviceName}' declares conflicting " +
                    "request binding shapes for '$typeName'."
            }
            first
        }.sortedBy(DotnetAspRequestBindingArtifact::typeName)

internal fun collectHeaderBindings(artifact: DotnetAspServiceArtifact): List<DotnetAspHeadersBindingArtifact> = artifact
    .endpoints
    .mapNotNull { it.bindings.headers }
    .groupBy(DotnetAspHeadersBindingArtifact::typeName)
    .map { (typeName, bindings) ->
        val first = bindings.first()
        require(bindings.all { it == first }) {
            "ASP.NET service '${artifact.serviceName}' declares conflicting " +
                "headers binding shapes for '$typeName'."
        }
        first
    }.sortedBy(DotnetAspHeadersBindingArtifact::typeName)
