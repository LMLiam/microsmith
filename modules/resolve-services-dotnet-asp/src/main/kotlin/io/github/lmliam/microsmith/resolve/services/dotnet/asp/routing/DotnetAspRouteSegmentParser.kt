package io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing

import arrow.core.Either
import io.github.lmliam.microsmith.dsl.services.dotnet.validation.isDotnetIdentifier
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue

internal class DotnetAspRouteSegmentParser {
    fun parse(
        segment: String,
        route: String,
        kind: DotnetAspResolutionIssue.RouteDeclarationKind,
    ): Either<DotnetAspResolutionIssue, DotnetAspRouteSegment> {
        if (!containsRouteSyntax(segment)) {
            return Either.Right(DotnetAspRouteSegment.Literal(segment))
        }

        if (!isWholePlaceholder(segment)) {
            return invalid(kind, route, DotnetAspResolutionIssue.RouteProblem.InvalidSegment(segment))
        }

        val placeholder = segment.substring(1, segment.length - 1)

        if (placeholder.isBlank() || placeholder != placeholder.trim()) {
            return invalid(kind, route, DotnetAspResolutionIssue.RouteProblem.BlankOrPaddedPlaceholder(segment))
        }

        if (!isDotnetIdentifier(placeholder)) {
            return invalid(kind, route, DotnetAspResolutionIssue.RouteProblem.InvalidPlaceholderIdentifier(placeholder))
        }

        return Either.Right(DotnetAspRouteSegment.Placeholder(placeholder))
    }

    private fun containsRouteSyntax(segment: String): Boolean = '{' in segment || '}' in segment

    private fun isWholePlaceholder(segment: String): Boolean {
        if (!segment.startsWith("{") || !segment.endsWith("}")) {
            return false
        }

        val body = segment.substring(1, segment.length - 1)

        return '{' !in body && '}' !in body
    }

    private fun invalid(
        kind: DotnetAspResolutionIssue.RouteDeclarationKind,
        route: String,
        problem: DotnetAspResolutionIssue.RouteProblem,
    ): Either<DotnetAspResolutionIssue, DotnetAspRouteSegment> =
        Either.Left(DotnetAspResolutionIssue.InvalidRouteDeclaration(kind, route, problem))
}
