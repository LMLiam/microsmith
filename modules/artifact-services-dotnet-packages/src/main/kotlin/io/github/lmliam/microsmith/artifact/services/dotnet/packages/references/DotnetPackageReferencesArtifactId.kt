package io.github.lmliam.microsmith.artifact.services.dotnet.packages.references
import io.github.lmliam.microsmith.artifact.ArtifactId

data class DotnetPackageReferencesArtifactId(val serviceName: String) : ArtifactId<DotnetPackageReferencesArtifact> {
    override val artifactType = DotnetPackageReferencesArtifact::class
}
