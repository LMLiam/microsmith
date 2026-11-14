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
import io.github.lmliam.microsmith.resolve.services.dotnet.ResolvedDotnetService
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspService
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspGenerationResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.MICROSMITH_CONTROLLER_BASE_TYPE_NAME
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.dotnetAspControllerBaseTypeName
import io.github.lmliam.microsmith.resolve.services.dotnet.resolution.DotnetWorkspace
import io.github.lmliam.microsmith.resolve.services.dotnet.resolution.DotnetWorkspaceResolver
import java.nio.file.Path

/** Finalises the ASP.NET subset of the .NET workspace into a scaffold-ready model. */
class DotnetAspWorkspaceResolver(
    private val dotnetWorkspaceResolver: DotnetWorkspaceResolver = DotnetWorkspaceResolver(),
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

        return dotnetResolution.fold(
            // Workspace issues are reported by DotnetWorkspaceDomainResolver, so this resolver yields nothing.
            ifLeft = { Either.Right(DotnetAspWorkspace(emptyMap())) },
            ifRight = { dotnetWorkspace -> resolveServices(extension, dotnetWorkspace, aspServiceNames) },
        )
    }

    private fun resolveServices(
        extension: ServicesExtension,
        dotnetWorkspace: DotnetWorkspace,
        aspServiceNames: Set<String>,
    ): EitherNel<DotnetResolutionIssue, DotnetAspWorkspace> {
        val services = dotnetWorkspace.services.filterKeys(aspServiceNames::contains).values.sortedBy { it.name }
        val aspExtensions = services.associate { it.name to aspExtensionOf(extension, it.name) }

        val ports =
            DotnetAspPortAllocator.allocate(
                services.map { service ->
                    val declaredPorts = aspExtensions.getValue(service.name).ports
                    DotnetAspPortRequest(
                        serviceName = service.name,
                        solutionName = service.solution.name,
                        projectName = service.project,
                        http = declaredPorts?.http,
                        https = declaredPorts?.https,
                    )
                },
            )

        val rests = services.mapOrAccumulate { service ->
            restResolver
                .resolve(
                    serviceName = service.name,
                    models = service.models,
                    rest = aspExtensions.getValue(service.name).rest,
                )
                .bindNel()
        }

        return Either.zipOrAccumulate(ports, rests, controllerBaseTypeCollisions(services)) {
                allocatedPorts,
                resolvedRests,
                _ ->
                services.zip(resolvedRests) { service, rest ->
                    ResolvedDotnetAspService(
                        name = service.name,
                        solutionName = service.solution.name,
                        projectName = service.project,
                        targetFrameworkMoniker = service.target.moniker,
                        outputRoot = Path.of("dotnet", service.solution.name, service.project),
                        ports = allocatedPorts.getValue(service.name),
                        models = service.models,
                        rest = rest,
                    )
                }
            }
            .flatMap(::createWorkspace)
    }

    private fun controllerBaseTypeCollisions(
        services: List<ResolvedDotnetService>,
    ): EitherNel<DotnetResolutionIssue, Unit> {
        val issues = services.mapNotNull { service ->
            val typeName = dotnetAspControllerBaseTypeName(service.project)
            if (typeName == MICROSMITH_CONTROLLER_BASE_TYPE_NAME) {
                DotnetAspGenerationResolutionIssue.ControllerBaseTypeCollision(
                    service.name,
                    service.project,
                    typeName,
                )
            } else {
                null
            }
        }

        return issues.toNonEmptyListOrNull()?.let { Either.Left(it) } ?: Either.Right(Unit)
    }

    private fun aspExtensionOf(extension: ServicesExtension, serviceName: String): DotnetAspServiceExtension =
        checkNotNull(
            extension.require(serviceName).model.get<DotnetServiceExtension>()?.get<DotnetAspServiceExtension>(),
        )

    private fun createWorkspace(
        services: List<ResolvedDotnetAspService>,
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
