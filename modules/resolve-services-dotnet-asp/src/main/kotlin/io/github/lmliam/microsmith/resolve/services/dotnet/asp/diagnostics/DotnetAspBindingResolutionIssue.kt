package io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics

sealed interface DotnetAspBindingResolutionIssue : DotnetAspResolutionIssue {
    data class PathBindingWithoutPlaceholders(
        val serviceName: String,
        val operationName: String,
        val bindingName: String,
        val route: String,
    ) : DotnetAspBindingResolutionIssue

    data class MissingPathBinding(
        val serviceName: String,
        val operationName: String,
        val route: String,
        val placeholders: List<String>,
    ) : DotnetAspBindingResolutionIssue

    data class OptionalPathBindingField(
        val serviceName: String,
        val operationName: String,
        val bindingName: String,
        val fieldName: String,
    ) : DotnetAspBindingResolutionIssue

    data class DefaultedPathBindingField(
        val serviceName: String,
        val operationName: String,
        val bindingName: String,
        val fieldName: String,
    ) : DotnetAspBindingResolutionIssue

    data class PathBindingFieldMismatch(
        val serviceName: String,
        val operationName: String,
        val bindingName: String,
        val placeholders: List<String>,
        val fields: List<String>,
    ) : DotnetAspBindingResolutionIssue

    data class RequestBindingReferenceField(
        val serviceName: String,
        val operationName: String,
        val bindingName: String,
        val fieldName: String,
        val targetName: String,
    ) : DotnetAspBindingResolutionIssue

    sealed interface ModelReferenceSource {
        data object RequestBody : ModelReferenceSource
        data class Response(val statusCode: Int) : ModelReferenceSource
        data class InlineModelField(val modelName: String, val fieldName: String) : ModelReferenceSource
    }

    data class UnknownSharedModelReference(
        val serviceName: String,
        val operationName: String,
        val source: ModelReferenceSource,
        val targetName: String,
    ) : DotnetAspBindingResolutionIssue
}
