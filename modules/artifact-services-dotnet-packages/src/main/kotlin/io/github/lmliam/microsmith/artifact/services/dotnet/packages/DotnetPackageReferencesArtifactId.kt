package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.core.ArtifactId

data class DotnetPackageReferencesArtifactId(val serviceName: String) : ArtifactId<DotnetPackageReferencesArtifact> {
    override val artifactType = DotnetPackageReferencesArtifact::class
}
