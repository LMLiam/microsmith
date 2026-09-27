package io.github.lmliam.microsmith.resolve.services.dotnet.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.mapOrAccumulate
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.services.Service
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.defaults.DotnetDefaultsExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.service.DotnetServiceExtension
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetWorkspaceResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.ResolvedDotnetService

/** Normalises the shared and per-service .NET DSL into a resolved workspace model. */
class DotnetWorkspaceResolver {
    fun resolve(extension: ServicesExtension): EitherNel<DotnetWorkspaceResolutionIssue, DotnetWorkspace> {
        val defaults = extension.get<DotnetDefaultsExtension>() ?: DotnetDefaultsExtension()
        val services =
            extension.services
                .mapNotNull { service -> service.model.get<DotnetServiceExtension>()?.let { service to it } }
                .sortedBy { (service) -> service.name }

        return services
            .mapOrAccumulate { (service, dotnet) ->
                resolveService(
                        service = service,
                        dotnet = dotnet,
                        defaults = defaults,
                    )
                    .bindNel()
            }
            .map { resolvedServices ->
                DotnetWorkspace(
                    target = defaults.target,
                    solutions = defaults.solutions,
                    services = resolvedServices.associateBy(ResolvedDotnetService::name),
                )
            }
    }

    private fun resolveService(
        service: Service,
        dotnet: DotnetServiceExtension,
        defaults: DotnetDefaultsExtension,
    ): EitherNel<DotnetWorkspaceResolutionIssue, ResolvedDotnetService> {
        val target = dotnet.target ?: defaults.target
        val solutionName = dotnet.solution
        val solution = solutionName?.let(defaults::findSolution)
        val project = dotnet.project
        val issues = buildList {
            if (target == null) add(DotnetWorkspaceResolutionIssue.TargetNotConfigured(service.name))

            when {
                solutionName == null -> add(DotnetWorkspaceResolutionIssue.SolutionNotConfigured(service.name))
                solution == null -> add(DotnetWorkspaceResolutionIssue.SolutionNotDeclared(service.name, solutionName))
            }

            if (project == null) add(DotnetWorkspaceResolutionIssue.ProjectNotConfigured(service.name))

            addAll(
                modelReferenceIssues(
                    service = service,
                    models = dotnet.models.values.toList(),
                )
            )
        }
        val accumulatedIssues = issues.toNonEmptyListOrNull()
        if (accumulatedIssues != null) return Either.Left(accumulatedIssues)

        return Either.Right(
            ResolvedDotnetService(
                name = service.name,
                target = checkNotNull(target),
                solution = checkNotNull(solution),
                project = checkNotNull(project),
                models = dotnet.models,
            )
        )
    }

    private fun modelReferenceIssues(
        service: Service,
        models: List<DotnetModel>,
    ): List<DotnetWorkspaceResolutionIssue> {
        val modelNames = models.mapTo(mutableSetOf(), DotnetModel::name)

        return buildList {
            models.forEach { model ->
                model.fields
                    .mapNotNull { it.type as? DotnetFieldType.Reference }
                    .filter { it.target !in modelNames }
                    .forEach { reference ->
                        add(
                            DotnetWorkspaceResolutionIssue.UnknownModelReference(
                                serviceName = service.name,
                                modelName = model.name,
                                targetName = reference.target,
                            )
                        )
                    }
            }
        }
    }
}
