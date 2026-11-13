package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.services.dotnet.packages.references.DotnetPackageReferencesArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.references.DotnetPackageReferencesContribution
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageService
import tech.mappie.api.ObjectMappie

internal object DotnetPackageReferencesContributionMapper :
    ObjectMappie<ResolvedDotnetPackageService, DotnetPackageReferencesContribution>() {
    override fun map(from: ResolvedDotnetPackageService): DotnetPackageReferencesContribution = mapping {
        to::artifactId fromProperty from::name transform { DotnetPackageReferencesArtifactId(it) }
        to::solutionName fromProperty from::solution
        to::projectName fromProperty from::project
        to::packages fromProperty from::packages transform { DotnetPackageReferenceMapper.mapList(it) }
    }
}
