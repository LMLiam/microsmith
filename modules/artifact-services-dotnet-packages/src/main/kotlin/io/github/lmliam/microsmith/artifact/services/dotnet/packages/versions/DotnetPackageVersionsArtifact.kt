package io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackagesArtifact

data class DotnetPackageVersionsArtifact(
    override val id: DotnetPackageVersionsArtifactId,
    val packages: List<DotnetPackageVersion>,
) : DotnetPackagesArtifact
