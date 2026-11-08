package io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper

@ServiceProvider(ResolutionIssueDiagnosticMapper::class)
class DotnetAspResolutionDiagnosticMapper : ResolutionIssueDiagnosticMapper<DotnetAspResolutionIssue> {
    override val issueType = DotnetAspResolutionIssue::class

    override fun map(issue: DotnetAspResolutionIssue): ResolutionDiagnostic =
        when (issue) {
            is DotnetAspResolutionIssue.DuplicateOperationName ->
                ResolutionDiagnostic(
                    code = "dotnet.asp.duplicate-operation-name",
                    message =
                        "ASP.NET service '${issue.serviceName}' declares duplicate operation name " +
                            "'${issue.operationName}'.",
                )

            is DotnetAspResolutionIssue.DuplicateRestEndpoint ->
                ResolutionDiagnostic(
                    code = "dotnet.asp.duplicate-rest-endpoint",
                    message =
                        "ASP.NET service '${issue.serviceName}' declares duplicate REST endpoint: " +
                            "${issue.method} ${issue.route}.",
                )

            is DotnetAspResolutionIssue.OutputRootCollision ->
                ResolutionDiagnostic(
                    code = "dotnet.asp.output-root-collision",
                    message =
                        "ASP.NET services ${issue.serviceNames.joinToString(", ")} resolve " +
                            "to colliding output root '${issue.outputRoot}'.",
                )

            is DotnetAspResolutionIssue.InvalidRouteDeclaration -> mapInvalidRoute(issue)

            is DotnetAspResolutionIssue.DuplicateRoutePlaceholders ->
                ResolutionDiagnostic(
                    code = "dotnet.asp.route.duplicate-placeholders",
                    message =
                        "ASP.NET route '${issue.route}' in operation '${issue.operationName}' for service " +
                            "'${issue.serviceName}' declares duplicate placeholders: " +
                            "${issue.placeholders.joinToString(", ")}.",
                )

            is DotnetAspBindingResolutionIssue -> issue.toDiagnostic()
        }

    private fun mapInvalidRoute(issue: DotnetAspResolutionIssue.InvalidRouteDeclaration): ResolutionDiagnostic {
        val label = issue.kind.displayName()

        return when (val problem = issue.problem) {
            DotnetAspResolutionIssue.RouteProblem.Blank ->
                ResolutionDiagnostic(
                    code = "dotnet.asp.route.blank",
                    message = "$label cannot be blank.",
                )

            DotnetAspResolutionIssue.RouteProblem.MissingLeadingSlash ->
                ResolutionDiagnostic(
                    code = "dotnet.asp.route.missing-leading-slash",
                    message = "$label must start with '/': '${issue.route}'.",
                )

            DotnetAspResolutionIssue.RouteProblem.EmptyPathSegment ->
                ResolutionDiagnostic(
                    code = "dotnet.asp.route.empty-segment",
                    message = "$label cannot contain empty path segments: '${issue.route}'.",
                )

            is DotnetAspResolutionIssue.RouteProblem.InvalidSegment ->
                ResolutionDiagnostic(
                    code = "dotnet.asp.route.invalid-segment",
                    message =
                        "$label contains invalid route segment '${problem.segment}' in '${issue.route}'. " +
                            "Placeholders must occupy a whole segment like '/{id}'.",
                )

            is DotnetAspResolutionIssue.RouteProblem.BlankOrPaddedPlaceholder ->
                ResolutionDiagnostic(
                    code = "dotnet.asp.route.invalid-placeholder",
                    message = "$label contains blank or padded placeholder '${problem.segment}' in '${issue.route}'.",
                )

            is DotnetAspResolutionIssue.RouteProblem.InvalidPlaceholderIdentifier ->
                ResolutionDiagnostic(
                    code = "dotnet.asp.route.invalid-placeholder-identifier",
                    message = "$label contains invalid route placeholder '${problem.placeholder}' in '${issue.route}'.",
                )
        }
    }

    private fun DotnetAspResolutionIssue.RouteDeclarationKind.displayName(): String =
        when (this) {
            DotnetAspResolutionIssue.RouteDeclarationKind.GROUP -> "Route group"
            DotnetAspResolutionIssue.RouteDeclarationKind.ENDPOINT -> "Endpoint route"
        }
}
