package io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspHttpMethod
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import java.nio.file.Path

sealed interface DotnetAspResolutionIssue : DotnetResolutionIssue {
    enum class RouteDeclarationKind {
        GROUP,
        ENDPOINT,
    }

    sealed interface RouteProblem {
        data object Blank : RouteProblem
        data object MissingLeadingSlash : RouteProblem
        data object EmptyPathSegment : RouteProblem
        data class InvalidSegment(val segment: String) : RouteProblem
        data class BlankOrPaddedPlaceholder(val segment: String) : RouteProblem
        data class InvalidPlaceholderIdentifier(val placeholder: String) : RouteProblem
    }

    data class InvalidRouteDeclaration(val kind: RouteDeclarationKind, val route: String, val problem: RouteProblem) :
        DotnetAspResolutionIssue

    data class DuplicateRoutePlaceholders(
        val serviceName: String,
        val operationName: String,
        val route: String,
        val placeholders: List<String>,
    ) : DotnetAspResolutionIssue

    data class DuplicateOperationName(val serviceName: String, val operationName: String) : DotnetAspResolutionIssue

    data class DuplicateRestEndpoint(val serviceName: String, val method: DotnetAspHttpMethod, val route: String) :
        DotnetAspResolutionIssue

    data class OutputRootCollision(val outputRoot: Path, val serviceNames: List<String>) : DotnetAspResolutionIssue
}
