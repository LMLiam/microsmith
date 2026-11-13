package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions.DotnetPackageVersionsArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions.DotnetPackageVersionsContribution
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.ResolvedDotnetPackageSolution
import tech.mappie.api.ObjectMappie

internal object DotnetPackageVersionsContributionMapper :
    ObjectMappie<ResolvedDotnetPackageSolution, DotnetPackageVersionsContribution>() {
    override fun map(from: ResolvedDotnetPackageSolution): DotnetPackageVersionsContribution = mapping {
        to::artifactId fromProperty from::name transform { DotnetPackageVersionsArtifactId(it) }
        to::packages fromProperty from::packages transform { DotnetPackageVersionMapper.mapList(it) }
    }
}
