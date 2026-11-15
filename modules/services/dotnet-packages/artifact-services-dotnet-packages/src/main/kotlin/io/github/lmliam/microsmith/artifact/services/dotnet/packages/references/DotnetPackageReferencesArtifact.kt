package io.github.lmliam.microsmith.artifact.services.dotnet.packages.references

import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackagesArtifact

public data class DotnetPackageReferencesArtifact(
    override val id: DotnetPackageReferencesArtifactId,
    public val solutionName: String,
    public val projectName: String,
    public val packages: List<DotnetPackageReference>,
) : DotnetPackagesArtifact
