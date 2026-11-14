package io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics

sealed interface DotnetAspGenerationResolutionIssue : DotnetAspResolutionIssue {
    data class ResponseHeaderCollidesWithBody(
        val serviceName: String,
        val operationName: String,
        val statusCode: Int,
        val headerName: String,
    ) : DotnetAspGenerationResolutionIssue

    data class ResponseHeaderPropertyCollision(
        val serviceName: String,
        val operationName: String,
        val statusCode: Int,
        val propertyNames: List<String>,
    ) : DotnetAspGenerationResolutionIssue

    data class ResponseHeaderWithoutPropertyName(
        val serviceName: String,
        val operationName: String,
        val statusCode: Int,
        val headerName: String,
    ) : DotnetAspGenerationResolutionIssue

    data class NoContentResponseWithBody(
        val serviceName: String,
        val operationName: String,
        val modelName: String,
    ) : DotnetAspGenerationResolutionIssue

    data class ControllerBaseTypeCollision(
        val serviceName: String,
        val projectName: String,
        val typeName: String,
    ) : DotnetAspGenerationResolutionIssue

    data class ContractTypeCollision(val serviceName: String, val typeName: String, val owners: List<String>) :
        DotnetAspGenerationResolutionIssue
}
