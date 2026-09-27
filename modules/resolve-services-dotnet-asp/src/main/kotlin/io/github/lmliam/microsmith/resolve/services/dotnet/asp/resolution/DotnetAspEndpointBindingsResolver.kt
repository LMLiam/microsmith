package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution
import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspRequestBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpointBindings
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRoute
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspBindingResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue

internal class DotnetAspEndpointBindingsResolver {
    private val bindingResolver = DotnetAspBindingResolver()
    private val modelResolver = DotnetAspModelResolver()

    fun resolve(
        context: DotnetAspOperationContext,
        endpoint: DotnetAspEndpoint,
        route: ResolvedDotnetAspRoute,
        models: Map<String, DotnetModel>,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspEndpointBindings> {
        val issues = mutableListOf<DotnetAspResolutionIssue>()

        var path: ResolvedDotnetAspRequestBinding? = null
        var query: ResolvedDotnetAspRequestBinding? = null
        var body: ResolvedDotnetAspModel? = null

        resolvePathBinding(context, route, endpoint.bindings.path).fold(
            ifLeft = issues::addAll,
            ifRight = { path = it },
        )

        endpoint.bindings.query?.let { binding ->
            bindingResolver.resolveRequestBinding(context, binding).fold(
                ifLeft = issues::addAll,
                ifRight = { query = it },
            )
        }

        endpoint.bindings.body?.let { reference ->
            modelResolver.resolve(
                context,
                models,
                reference,
                DotnetAspBindingResolutionIssue.ModelReferenceSource.RequestBody,
            ).fold(
                ifLeft = issues::addAll,
                ifRight = { body = it },
            )
        }

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        if (accumulatedIssues != null) return Either.Left(accumulatedIssues)

        return Either.Right(
            ResolvedDotnetAspEndpointBindings(
                path,
                query,
                headers = endpoint.bindings.headers?.let(bindingResolver::resolveHeadersBinding),
                body,
            ),
        )
    }

    private fun resolvePathBinding(
        context: DotnetAspOperationContext,
        route: ResolvedDotnetAspRoute,
        binding: DotnetAspRequestBinding?,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspRequestBinding?> {
        if (binding == null) {
            return if (route.placeholders.isEmpty()) {
                Either.Right(null)
            } else {
                Either.Left(
                    nonEmptyListOf(
                        DotnetAspBindingResolutionIssue.MissingPathBinding(
                            context.serviceName,
                            context.operationName,
                            route.path,
                            route.placeholders,
                        ),
                    ),
                )
            }
        }

        if (route.placeholders.isEmpty()) {
            return Either.Left(
                nonEmptyListOf(
                    DotnetAspBindingResolutionIssue.PathBindingWithoutPlaceholders(
                        context.serviceName,
                        context.operationName,
                        binding.name,
                        route.path,
                    ),
                ),
            )
        }

        return bindingResolver.resolvePathBinding(context, route.placeholders, binding)
    }
}
