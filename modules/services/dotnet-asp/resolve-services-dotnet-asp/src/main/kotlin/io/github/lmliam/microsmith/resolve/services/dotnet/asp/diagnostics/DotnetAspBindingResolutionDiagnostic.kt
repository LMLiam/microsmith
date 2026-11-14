package io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic

internal fun DotnetAspBindingResolutionIssue.toDiagnostic(): ResolutionDiagnostic =
    when (this) {
        is DotnetAspBindingResolutionIssue.PathBindingWithoutPlaceholders ->
            ResolutionDiagnostic(
                code = "dotnet.asp.path-binding-without-placeholders",
                message =
                    "ASP.NET endpoint '$operationName' in service '$serviceName' declares path binding " +
                        "'$bindingName' but route '$route' has no placeholders.",
            )

        is DotnetAspBindingResolutionIssue.MissingPathBinding ->
            ResolutionDiagnostic(
                code = "dotnet.asp.path-binding-required",
                message =
                    "ASP.NET endpoint '$operationName' in service '$serviceName' must declare a path " +
                        "binding for route '$route'.",
            )

        is DotnetAspBindingResolutionIssue.OptionalPathBindingField ->
            ResolutionDiagnostic(
                code = "dotnet.asp.path-binding-field-optional",
                message =
                    "ASP.NET path binding '$bindingName' field '$fieldName' in operation '$operationName' " +
                        "cannot be optional.",
            )

        is DotnetAspBindingResolutionIssue.DefaultedPathBindingField ->
            ResolutionDiagnostic(
                code = "dotnet.asp.path-binding-field-defaulted",
                message =
                    "ASP.NET path binding '$bindingName' field '$fieldName' in operation '$operationName' " +
                        "cannot declare a default value.",
            )

        is DotnetAspBindingResolutionIssue.PathBindingFieldMismatch ->
            ResolutionDiagnostic(
                code = "dotnet.asp.path-binding-field-mismatch",
                message =
                    "ASP.NET path binding '$bindingName' in operation '$operationName' must match route placeholders " +
                        "${placeholders.joinToString(", ")}, but declared ${fields.joinToString(", ")}.",
            )

        is DotnetAspBindingResolutionIssue.RequestBindingReferenceField ->
            ResolutionDiagnostic(
                code = "dotnet.asp.request-binding-reference-field",
                message =
                    "ASP.NET request binding '$bindingName' field '$fieldName' in operation '$operationName' " +
                        "cannot reference shared model '$targetName'. Transport bindings must declare scalar fields.",
            )

        is DotnetAspBindingResolutionIssue.IntegerDefaultNotIntegral ->
            ResolutionDiagnostic(
                code = "dotnet.asp.request-binding-default-not-integral",
                message =
                    "ASP.NET request binding '$bindingName' field '$fieldName' in operation '$operationName' " +
                        "declares default '$value', which is not representable as integer type '$csharpTypeName'.",
            )

        is DotnetAspBindingResolutionIssue.IntegerDefaultOutOfRange ->
            ResolutionDiagnostic(
                code = "dotnet.asp.request-binding-default-out-of-range",
                message =
                    "ASP.NET request binding '$bindingName' field '$fieldName' in operation '$operationName' " +
                        "declares default '$value', which is out of range for integer type '$csharpTypeName'.",
            )

        is DotnetAspBindingResolutionIssue.UnknownSharedModelReference ->
            ResolutionDiagnostic(
                code = "dotnet.asp.unknown-shared-model-reference",
                message =
                    "ASP.NET ${source.displayName()} in operation '$operationName' for service '$serviceName' " +
                        "references unknown shared model '$targetName'.",
            )
    }

private fun DotnetAspBindingResolutionIssue.ModelReferenceSource.displayName(): String =
    when (this) {
        DotnetAspBindingResolutionIssue.ModelReferenceSource.RequestBody -> "request body"

        is DotnetAspBindingResolutionIssue.ModelReferenceSource.Response -> "response $statusCode"

        is DotnetAspBindingResolutionIssue.ModelReferenceSource.InlineModelField ->
            "inline model '$modelName' field '$fieldName'"
    }
