package io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRoute
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution.DotnetAspOperationContext

internal class DotnetAspRouteResolver {
    private val segmentParser = DotnetAspRouteSegmentParser()

    fun parseDeclaredRoute(
        route: String,
        kind: DotnetAspResolutionIssue.RouteDeclarationKind,
        allowEmpty: Boolean = false,
    ): EitherNel<DotnetAspResolutionIssue, DotnetAspRouteFragment> {
        val normalized = route.trim()

        if (normalized.isEmpty()) {
            return if (allowEmpty) {
                Either.Right(DotnetAspRouteFragment.Empty)
            } else {
                invalidRoute(kind, route, DotnetAspResolutionIssue.RouteProblem.Blank)
            }
        }

        val issues = mutableListOf<DotnetAspResolutionIssue>()

        if (!normalized.startsWith("/")) {
            issues +=
                DotnetAspResolutionIssue.InvalidRouteDeclaration(
                    kind,
                    route,
                    DotnetAspResolutionIssue.RouteProblem.MissingLeadingSlash,
                )
        }

        if ("//" in normalized) {
            issues +=
                DotnetAspResolutionIssue.InvalidRouteDeclaration(
                    kind,
                    route,
                    DotnetAspResolutionIssue.RouteProblem.EmptyPathSegment,
                )
        }

        val segments =
            normalized.split('/').filter(String::isNotBlank).mapNotNull { segment ->
                segmentParser
                    .parse(segment, route, kind)
                    .fold(
                        ifLeft = {
                            issues += it
                            null
                        },
                        ifRight = { it },
                    )
            }

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        return if (accumulatedIssues != null) {
            Either.Left(accumulatedIssues)
        } else {
            Either.Right(DotnetAspRouteFragment(segments))
        }
    }

    fun resolve(
        context: DotnetAspOperationContext,
        fragment: DotnetAspRouteFragment,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspRoute> {
        val path = if (fragment.segments.isEmpty()) "/" else "/" + fragment.segments.joinToString("/") { it.text }

        val placeholders =
            fragment.segments
                .filterIsInstance<DotnetAspRouteSegment.Placeholder>()
                .map(DotnetAspRouteSegment.Placeholder::name)

        val duplicates = placeholders.groupBy { it }.filterValues { it.size > 1 }.keys.sorted()

        return if (duplicates.isEmpty()) {
            Either.Right(ResolvedDotnetAspRoute(path, placeholders))
        } else {
            Either.Left(
                nonEmptyListOf(
                    DotnetAspResolutionIssue.DuplicateRoutePlaceholders(
                        context.serviceName,
                        context.operationName,
                        path,
                        duplicates,
                    )
                )
            )
        }
    }

    private fun invalidRoute(
        kind: DotnetAspResolutionIssue.RouteDeclarationKind,
        route: String,
        problem: DotnetAspResolutionIssue.RouteProblem,
    ): EitherNel<DotnetAspResolutionIssue, DotnetAspRouteFragment> =
        Either.Left(nonEmptyListOf(DotnetAspResolutionIssue.InvalidRouteDeclaration(kind, route, problem)))
}
