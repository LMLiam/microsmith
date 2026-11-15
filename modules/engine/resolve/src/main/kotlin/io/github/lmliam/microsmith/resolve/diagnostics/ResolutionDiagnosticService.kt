package io.github.lmliam.microsmith.resolve.diagnostics

import arrow.core.NonEmptyList
import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.resolve.ResolutionIssue

@InternalMicrosmithApi
public class ResolutionDiagnosticService(mappers: Iterable<ResolutionIssueDiagnosticMapper<*>> = emptyList()) {
    private val registry = ResolutionIssueDiagnosticMapperRegistry(mappers.toList())

    public fun describe(issue: ResolutionIssue): ResolutionDiagnostic =
        registry.resolve(issue)?.map(issue) ?: fallbackDiagnostic(issue)

    public fun describe(issues: NonEmptyList<ResolutionIssue>): NonEmptyList<ResolutionDiagnostic> =
        issues.map(::describe)

    private fun fallbackDiagnostic(issue: ResolutionIssue): ResolutionDiagnostic =
        ResolutionDiagnostic(
            code = "resolution.unmapped-issue",
            message = "Resolution failed with issue type " + "'${issue::class.qualifiedName ?: issue::class}'.",
        )
}
