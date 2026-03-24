package io.github.lmliam.microsmith.gen.services.dotnet

import io.github.lmliam.microsmith.dsl.services.core.Service
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.shared.DotnetSharedExtension

/**
 * Normalises the shared and per-service .NET DSL into a resolved workspace model.
 */
class DotnetGenerationService {
    fun resolve(extension: ServicesExtension): DotnetWorkspace {
        val shared = extension.get<DotnetSharedExtension>() ?: DotnetSharedExtension()

        val services =
            extension.services.mapNotNull { service ->
                service.model.get<DotnetServiceExtension>()?.let { dotnet ->
                    resolveService(service, dotnet, shared)
                }
            }.associateBy(ResolvedDotnetService::name)

        return DotnetWorkspace(
            target = shared.target,
            solutions = shared.solutions,
            services = services,
        )
    }

    private fun resolveService(
        service: Service,
        dotnet: DotnetServiceExtension,
        shared: DotnetSharedExtension,
    ): ResolvedDotnetService {
        val target =
            dotnet.target
                ?: shared.target
                ?: error("Dotnet target not configured for service '${service.name}'.")
        val solutionName = dotnet.solution ?: error("Dotnet solution not configured for service '${service.name}'.")
        val solution =
            shared.findSolution(solutionName)
                ?: throw DotnetGenerationErrors.solutionNotDeclared(service.name, solutionName)
        val project = dotnet.project ?: error("Dotnet project not configured for service '${service.name}'.")

        validateModelReferences(service, dotnet.models.values.toList())

        return ResolvedDotnetService(
            name = service.name,
            target = target,
            solution = solution,
            project = project,
            models = dotnet.models,
        )
    }

    private fun validateModelReferences(service: Service, models: List<DotnetModel>) {
        val modelNames = models.mapTo(mutableSetOf(), DotnetModel::name)

        models.forEach { model ->
            model.fields
                .mapNotNull { it.type as? DotnetFieldType.Reference }
                .forEach { reference ->
                    require(reference.target in modelNames) {
                        "Dotnet model '${model.name}' in service '${service.name}' " +
                            "references unknown model '${reference.target}'."
                    }
                }
        }
    }
}
