package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponseHeader
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspBindingResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue

internal class DotnetAspResponseResolver {
    private val modelResolver = DotnetAspModelResolver()

    fun resolve(
        context: DotnetAspOperationContext,
        endpoint: DotnetAspEndpoint,
        models: Map<String, DotnetModel>,
    ): EitherNel<DotnetAspResolutionIssue, List<ResolvedDotnetAspResponse>> {
        val issues = mutableListOf<DotnetAspResolutionIssue>()
        val resolved = mutableListOf<ResolvedDotnetAspResponse>()

        endpoint.responses.forEach { response ->
            modelResolver.resolve(
                context,
                models,
                response.model,
                DotnetAspBindingResolutionIssue.ModelReferenceSource.Response(response.statusCode),
            ).fold(
                ifLeft = issues::addAll,
                ifRight = { model ->
                    resolved +=
                        ResolvedDotnetAspResponse(
                            response.statusCode,
                            model,
                            headers = response.headers.map {
                                ResolvedDotnetAspResponseHeader(it.name)
                            },
                        )
                },
            )
        }

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        return if (accumulatedIssues != null) {
            Either.Left(accumulatedIssues)
        } else {
            Either.Right(resolved)
        }
    }
}
