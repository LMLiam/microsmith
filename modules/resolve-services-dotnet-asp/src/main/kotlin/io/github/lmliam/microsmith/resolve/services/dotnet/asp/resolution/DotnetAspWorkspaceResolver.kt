package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.mapOrAccumulate
import arrow.core.raise.context.bindNel
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.DotnetAspServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.service.DotnetServiceExtension
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspPorts
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspService
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.resolution.DotnetWorkspace
import io.github.lmliam.microsmith.resolve.services.dotnet.resolution.DotnetWorkspaceResolver
import java.nio.file.Path

/** Finalises the ASP.NET subset of the .NET workspace into a scaffold-ready model. */
class DotnetAspWorkspaceResolver(
    private val dotnetWorkspaceResolver: DotnetWorkspaceResolver = DotnetWorkspaceResolver()
) {
    private val restResolver = DotnetAspRestResolver()

    fun resolve(extension: ServicesExtension): EitherNel<DotnetResolutionIssue, DotnetAspWorkspace> {
        val aspServiceNames =
            extension.services
                .filter { service ->
                    service.model.get<DotnetServiceExtension>()?.get<DotnetAspServiceExtension>() != null
                }
                .map { it.name }
                .toSet()

        if (aspServiceNames.isEmpty()) return Either.Right(DotnetAspWorkspace(emptyMap()))

        val dotnetResolution: EitherNel<DotnetResolutionIssue, DotnetWorkspace> =
            dotnetWorkspaceResolver.resolve(extension)

        return dotnetResolution.flatMap { dotnetWorkspace ->
            dotnetWorkspace.services
                .filterKeys(aspServiceNames::contains)
                .values
                .sortedBy { it.name }
                .mapOrAccumulate { resolvedService ->
                    val aspExtension =
                        checkNotNull(
                            extension
                                .require(resolvedService.name)
                                .model
                                .get<DotnetServiceExtension>()
                                ?.get<DotnetAspServiceExtension>()
                        )

                    val rest =
                        restResolver
                            .resolve(
                                serviceName = resolvedService.name,
                                models = resolvedService.models,
                                rest = aspExtension.rest,
                            )
                            .bindNel()

                    ResolvedDotnetAspService(
                        name = resolvedService.name,
                        solutionName = resolvedService.solution.name,
                        projectName = resolvedService.project,
                        targetFrameworkMoniker = resolvedService.target.moniker,
                        outputRoot = Path.of("dotnet", resolvedService.solution.name, resolvedService.project),
                        ports =
                            aspExtension.ports?.let {
                                ResolvedDotnetAspPorts(
                                    http = it.http,
                                    https = it.https,
                                )
                            },
                        models = resolvedService.models,
                        rest = rest,
                    )
                }
                .flatMap(::createWorkspace)
        }
    }

    private fun createWorkspace(
        services: List<ResolvedDotnetAspService>
    ): EitherNel<DotnetResolutionIssue, DotnetAspWorkspace> {
        val issues =
            services
                .groupBy { it.outputRoot.normalize() }
                .filterValues { it.size > 1 }
                .map { (outputRoot, collidingServices) ->
                    DotnetAspResolutionIssue.OutputRootCollision(
                        outputRoot = outputRoot,
                        serviceNames = collidingServices.map(ResolvedDotnetAspService::name).sorted(),
                    )
                }
                .sortedBy { it.outputRoot.toString() }

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        return if (accumulatedIssues != null) {
            Either.Left(accumulatedIssues)
        } else {
            Either.Right(DotnetAspWorkspace(services.associateBy(ResolvedDotnetAspService::name)))
        }
    }
}
