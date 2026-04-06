package io.github.lmliam.microsmith.resolve.services.dotnet.packages

import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults.DotnetDefaultsExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.DotnetPackageReferenceDeclaration
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.DotnetPackageReferencesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.DotnetPackageVersionsExtension
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetWorkspace
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

        val solutionsByName = resolveSolutionsByName(defaults)
        val servicesByName = resolveServicesByName(extension, dotnetWorkspace, solutionsByName)

        return DotnetPackageWorkspace(
            solutionsByName = solutionsByName,
            servicesByName = servicesByName,
        )
    }

    private fun resolveServicesByName(
        extension: ServicesExtension,
        dotnetWorkspace: DotnetWorkspace,
        solutionsByName: Map<String, ResolvedDotnetPackageSolution>,
    ): Map<String, ResolvedDotnetPackageService> {
        return dotnetWorkspace.services.values.mapNotNull { resolvedService ->
            val service = extension.require(resolvedService.name)
            val dotnet = service.model.get<DotnetServiceExtension>() ?: return@mapNotNull null
            val references = dotnet.get<DotnetPackageReferencesExtension>() ?: return@mapNotNull null
            if (references.packages.isEmpty()) {
                return@mapNotNull null
            }

            ResolvedDotnetPackageService(
                name = service.name,
                solution = resolvedService.solution.name,
                project = resolvedService.project,
                packages = resolveServicePackages(
                    serviceName = service.name,
                    solutionName = resolvedService.solution.name,
                    references = references.packages,
                    centrallyManagedPackagesByName =
                    solutionsByName[resolvedService.solution.name]
                        ?.packageVersionsByName()
                        .orEmpty(),
                ),
            )
        }.associateBy(ResolvedDotnetPackageService::name)
    }

    private fun resolveServicePackages(
        serviceName: String,
        solutionName: String,
        references: List<DotnetPackageReferenceDeclaration>,
        centrallyManagedPackagesByName: Map<String, ResolvedDotnetPackageVersion>,
    ): List<ResolvedDotnetPackageReference> = references.map { reference ->
        resolveServicePackageReference(
            serviceName = serviceName,
            solutionName = solutionName,
            reference = reference,
            centrallyManagedPackagesByName = centrallyManagedPackagesByName,
        )
    }

    private fun resolveServicePackageReference(
        serviceName: String,
        solutionName: String,
        reference: DotnetPackageReferenceDeclaration,
        centrallyManagedPackagesByName: Map<String, ResolvedDotnetPackageVersion>,
    ): ResolvedDotnetPackageReference {
        val usesCentralPackageManagement = centrallyManagedPackagesByName.isNotEmpty()

        return when {
            reference.version != null && usesCentralPackageManagement ->
                DotnetPackageWorkspaceResolutionErrors.mixedPackageVersionManagement(
                    serviceName,
                    solutionName,
                    reference.name,
                )

            reference.version != null ->
                ResolvedDotnetPackageReference(
                    name = reference.name,
                    version = reference.version,
                )

            usesCentralPackageManagement -> {
                centrallyManagedPackagesByName[reference.name]
                    ?: DotnetPackageWorkspaceResolutionErrors.packageNotDeclared(
                        serviceName,
                        solutionName,
                        reference.name,
                    )
                ResolvedDotnetPackageReference(
                    name = reference.name,
                    version = null,
                )
            }

            else ->
                DotnetPackageWorkspaceResolutionErrors.packageVersionRequired(
                    serviceName,
                    reference.name,
                )
        }
    }

    private fun resolveSolutionsByName(defaults: DotnetDefaultsExtension): Map<String, ResolvedDotnetPackageSolution> {
        val solutionsByName = linkedMapOf<String, ResolvedDotnetPackageSolution>()

        defaults.allSolutions().forEach { solution ->
            val packageVersions = solution.get<DotnetPackageVersionsExtension>()?.packages.orEmpty()
            if (packageVersions.isEmpty()) {
                return@forEach
            }

            solutionsByName[solution.name] =
                ResolvedDotnetPackageSolution(
                    name = solution.name,
                    packages = packageVersions.map { packageVersion ->
                        ResolvedDotnetPackageVersion(
                            name = packageVersion.name,
                            version = packageVersion.version,
                        )
                    }.sortedBy(ResolvedDotnetPackageVersion::name),
                )
        }

        return solutionsByName
    }
}
