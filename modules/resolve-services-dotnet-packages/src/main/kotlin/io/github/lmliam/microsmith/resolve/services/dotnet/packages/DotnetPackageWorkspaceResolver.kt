package io.github.lmliam.microsmith.resolve.services.dotnet.packages

import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults.DotnetDefaultsExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.DotnetPackageReferencesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.DotnetPackageVersionsExtension
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetWorkspaceResolver

/**
 * Resolves the additive .NET package-management DSL into a validation-ready workspace model.
 */
class DotnetPackageWorkspaceResolver(
    private val dotnetWorkspaceResolver: DotnetWorkspaceResolver = DotnetWorkspaceResolver(),
) {
    fun resolve(extension: ServicesExtension): DotnetPackageWorkspace {
        val dotnetWorkspace = dotnetWorkspaceResolver.resolve(extension)
        val defaults = extension.get<DotnetDefaultsExtension>() ?: DotnetDefaultsExtension()

        val solutions = resolveSolutions(defaults)
        val services =
            dotnetWorkspace.services.values.mapNotNull { resolvedService ->
                val service = extension.require(resolvedService.name)
                val dotnet = service.model.get<DotnetServiceExtension>() ?: return@mapNotNull null
                val references = dotnet.get<DotnetPackageReferencesExtension>() ?: return@mapNotNull null
                if (references.packages.isEmpty()) {
                    return@mapNotNull null
                }

                val solutionPackages =
                    solutions[resolvedService.solution.name]?.packages.orEmpty()
                val usesCentralPackageManagement = solutionPackages.isNotEmpty()
                val resolvedPackages =
                    references.packages.toSortedMap().map { (packageName, declaredVersion) ->
                        when {
                            declaredVersion != null && usesCentralPackageManagement ->
                                DotnetPackageWorkspaceResolutionErrors.mixedPackageVersionManagement(
                                    service.name,
                                    resolvedService.solution.name,
                                    packageName,
                                )

                            declaredVersion != null ->
                                ResolvedDotnetPackageReference(
                                    name = packageName,
                                    version = declaredVersion,
                                )

                            usesCentralPackageManagement -> {
                                solutionPackages[packageName]
                                    ?: DotnetPackageWorkspaceResolutionErrors.packageNotDeclared(
                                        service.name,
                                        resolvedService.solution.name,
                                        packageName,
                                    )
                                ResolvedDotnetPackageReference(
                                    name = packageName,
                                    version = null,
                                )
                            }

                            else ->
                                DotnetPackageWorkspaceResolutionErrors.packageVersionRequired(
                                    service.name,
                                    packageName,
                                )
                        }
                    }

                ResolvedDotnetPackageService(
                    name = service.name,
                    solution = resolvedService.solution.name,
                    project = resolvedService.project,
                    packages = resolvedPackages,
                )
            }.associateBy(ResolvedDotnetPackageService::name)

        return DotnetPackageWorkspace(solutions = solutions, services = services)
    }

    private fun resolveSolutions(defaults: DotnetDefaultsExtension): Map<String, ResolvedDotnetPackageSolution> {
        val solutions = linkedMapOf<String, ResolvedDotnetPackageSolution>()

        defaults.allSolutions().forEach { solution ->
            val packageVersions = solution.get<DotnetPackageVersionsExtension>()?.packages.orEmpty()
            if (packageVersions.isEmpty()) {
                return@forEach
            }

            solutions[solution.name] =
                ResolvedDotnetPackageSolution(
                    name = solution.name,
                    packages = packageVersions,
                )
        }

        return solutions
    }
}
