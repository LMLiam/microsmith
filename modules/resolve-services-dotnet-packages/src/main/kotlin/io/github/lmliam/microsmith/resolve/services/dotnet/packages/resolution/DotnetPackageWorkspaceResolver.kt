package io.github.lmliam.microsmith.resolve.services.dotnet.packages.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.mapOrAccumulate
import arrow.core.raise.context.bind
import arrow.core.raise.context.bindNel
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.defaults.DotnetDefaultsExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.DotnetPackageReferenceDeclaration
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.DotnetPackageReferencesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.DotnetPackageVersionsExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.service.DotnetServiceExtension
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageReference
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageService
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageSolution
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageVersion
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.diagnostics.DotnetPackageWorkspaceResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.resolution.DotnetWorkspace
import io.github.lmliam.microsmith.resolve.services.dotnet.resolution.DotnetWorkspaceResolver

/**
 * Resolves the additive .NET package-management DSL into a validation-ready workspace model.
 */
class DotnetPackageWorkspaceResolver(
    private val dotnetWorkspaceResolver: DotnetWorkspaceResolver = DotnetWorkspaceResolver(),
) {
    fun resolve(extension: ServicesExtension): EitherNel<DotnetResolutionIssue, DotnetPackageWorkspace> {
        val defaults = extension.get<DotnetDefaultsExtension>() ?: DotnetDefaultsExtension()
        val solutionsByName = resolveSolutionsByName(defaults)

        return dotnetWorkspaceResolver
            .resolve(extension)
            .fold(
                ifLeft = { Either.Left(it) },
                ifRight = { dotnetWorkspace ->
                    resolveServicesByName(extension, dotnetWorkspace, solutionsByName)
                        .map { servicesByName ->
                            DotnetPackageWorkspace(
                                servicesByName = servicesByName,
                                solutionsByName = solutionsByName,
                            )
                        }
                },
            )
    }

    private fun resolveServicesByName(
        extension: ServicesExtension,
        dotnetWorkspace: DotnetWorkspace,
        solutionsByName: Map<String, ResolvedDotnetPackageSolution>,
    ): EitherNel<DotnetPackageWorkspaceResolutionIssue, Map<String, ResolvedDotnetPackageService>> =
        dotnetWorkspace.services.values
            .mapOrAccumulate { resolvedService ->
                val service = extension.require(resolvedService.name)
                val dotnet = service.model.get<DotnetServiceExtension>() ?: return@mapOrAccumulate null
                val references = dotnet.get<DotnetPackageReferencesExtension>() ?: return@mapOrAccumulate null
                if (references.packages.isEmpty()) return@mapOrAccumulate null
                val packages = resolveServicePackages(
                    serviceName = service.name,
                    solutionName = resolvedService.solution.name,
                    references = references.packages,
                    centrallyManagedPackagesByName = solutionsByName[resolvedService.solution.name]
                        ?.packageVersionsByName()
                        .orEmpty(),
                ).bindNel()

                ResolvedDotnetPackageService(
                    name = service.name,
                    solution = resolvedService.solution.name,
                    project = resolvedService.project,
                    packages = packages,
                )
            }
            .map {
                it
                    .filterNotNull()
                    .associateBy(ResolvedDotnetPackageService::name)
            }

    private fun resolveServicePackages(
        serviceName: String,
        solutionName: String,
        references: List<DotnetPackageReferenceDeclaration>,
        centrallyManagedPackagesByName: Map<String, ResolvedDotnetPackageVersion>,
    ): EitherNel<DotnetPackageWorkspaceResolutionIssue, List<ResolvedDotnetPackageReference>> =
        references.mapOrAccumulate { reference ->
            resolveServicePackageReference(
                serviceName = serviceName,
                solutionName = solutionName,
                reference = reference,
                centrallyManagedPackagesByName = centrallyManagedPackagesByName,
            ).bind()
        }

    private fun resolveServicePackageReference(
        serviceName: String,
        solutionName: String,
        reference: DotnetPackageReferenceDeclaration,
        centrallyManagedPackagesByName: Map<String, ResolvedDotnetPackageVersion>,
    ): Either<DotnetPackageWorkspaceResolutionIssue, ResolvedDotnetPackageReference> {
        val usesCentralPackageManagement = centrallyManagedPackagesByName.isNotEmpty()

        return when {
            reference.version != null && usesCentralPackageManagement -> Either.Left(
                DotnetPackageWorkspaceResolutionIssue.MixedPackageVersionManagement(
                    serviceName = serviceName,
                    solutionName = solutionName,
                    packageName = reference.name,
                ),
            )

            reference.version != null -> Either.Right(
                ResolvedDotnetPackageReference(
                    name = reference.name,
                    version = reference.version,
                ),
            )

            usesCentralPackageManagement && reference.name !in centrallyManagedPackagesByName -> Either.Left(
                DotnetPackageWorkspaceResolutionIssue.PackageNotCentrallyOwned(
                    serviceName = serviceName,
                    solutionName = solutionName,
                    packageName = reference.name,
                ),
            )

            usesCentralPackageManagement -> Either.Right(
                ResolvedDotnetPackageReference(
                    name = reference.name,
                    version = null,
                ),
            )

            else -> Either.Left(
                DotnetPackageWorkspaceResolutionIssue.PackageVersionRequired(
                    serviceName = serviceName,
                    packageName = reference.name,
                ),
            )
        }
    }

    private fun resolveSolutionsByName(defaults: DotnetDefaultsExtension): Map<String, ResolvedDotnetPackageSolution> {
        val solutionsByName = linkedMapOf<String, ResolvedDotnetPackageSolution>()

        defaults.allSolutions().forEach { solution ->
            val packageVersions = solution
                .get<DotnetPackageVersionsExtension>()
                ?.packages
                .orEmpty()
            if (packageVersions.isEmpty()) return@forEach

            solutionsByName[solution.name] = ResolvedDotnetPackageSolution(
                name = solution.name,
                packages = packageVersions
                    .map {
                        ResolvedDotnetPackageVersion(
                            name = it.name,
                            version = it.version,
                        )
                    }
                    .sortedBy(ResolvedDotnetPackageVersion::name),
            )
        }

        return solutionsByName
    }
}
