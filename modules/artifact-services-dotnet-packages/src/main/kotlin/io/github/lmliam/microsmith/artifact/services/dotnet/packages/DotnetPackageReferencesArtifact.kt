package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.services.dotnet.packages.core.DotnetPackagesArtifact

data class DotnetPackageReferencesArtifact(
    override val id: DotnetPackageReferencesArtifactId,
    val solutionName: String,
    val projectName: String,
    val packages: List<DotnetPackageReference>,
) : DotnetPackagesArtifact
