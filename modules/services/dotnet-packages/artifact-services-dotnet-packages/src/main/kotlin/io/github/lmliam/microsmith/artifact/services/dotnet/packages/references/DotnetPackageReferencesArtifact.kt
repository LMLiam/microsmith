package io.github.lmliam.microsmith.artifact.services.dotnet.packages.references

import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackagesArtifact

data class DotnetPackageReferencesArtifact(
    override val id: DotnetPackageReferencesArtifactId,
    val solutionName: String,
    val projectName: String,
    val packages: List<DotnetPackageReference>,
) : DotnetPackagesArtifact
