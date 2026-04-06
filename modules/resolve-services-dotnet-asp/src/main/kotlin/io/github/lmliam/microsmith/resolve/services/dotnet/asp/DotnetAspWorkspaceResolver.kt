package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service.DotnetAspServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetWorkspaceResolver
import java.nio.file.Path

/**
 * Finalises the ASP.NET subset of the .NET workspace into a scaffold-ready model.
 */
class DotnetAspWorkspaceResolver(
    private val dotnetWorkspaceResolver: DotnetWorkspaceResolver = DotnetWorkspaceResolver(),
) {
    private val restResolver = DotnetAspRestResolver()

    fun resolve(extension: ServicesExtension): DotnetAspWorkspace {
        val aspServiceNames =
            extension.services
                .filter { service ->
                    service.model.get<DotnetServiceExtension>()?.get<DotnetAspServiceExtension>() != null
                }.map { it.name }
                .toSet()

        if (aspServiceNames.isEmpty()) {
            return DotnetAspWorkspace(emptyMap())
        }

        val dotnetWorkspace = dotnetWorkspaceResolver.resolve(extension)
        val services =
            dotnetWorkspace.services
                .filterKeys(aspServiceNames::contains)
                .values
                .sortedBy { it.name }
                .map { resolvedService ->
                    val aspExtension =
                        requireNotNull(
                            extension
                                .require(resolvedService.name)
                                .model
                                .get<DotnetServiceExtension>()
                                ?.get<DotnetAspServiceExtension>(),
                        )
                    ResolvedDotnetAspService(
                        name = resolvedService.name,
                        solutionName = resolvedService.solution.name,
                        projectName = resolvedService.project,
                        targetFrameworkMoniker = resolvedService.target.moniker,
                        outputRoot =
                        Path.of(
                            "dotnet",
                            resolvedService.solution.name,
                            resolvedService.project,
                        ),
                        ports = aspExtension.ports?.let {
                            ResolvedDotnetAspPorts(
                                http = it.http,
                                https = it.https,
                            )
                        },
                        models = resolvedService.models,
                        rest =
                        restResolver.resolve(
                            resolvedService.name,
                            resolvedService.models,
                            aspExtension.rest,
                        ),
                    )
                }

        val collisions =
            services
                .groupBy { it.outputRoot.normalize().toString() }
                .filterValues { it.size > 1 }
                .values
                .map { entries ->
                    entries
                        .map(ResolvedDotnetAspService::name)
                        .sorted()
                        .joinToString(", ")
                }.sorted()

        require(collisions.isEmpty()) {
            "ASP.NET services resolve to colliding output roots: ${collisions.joinToString("; ")}."
        }

        return DotnetAspWorkspace(services.associateBy(ResolvedDotnetAspService::name))
    }
}
