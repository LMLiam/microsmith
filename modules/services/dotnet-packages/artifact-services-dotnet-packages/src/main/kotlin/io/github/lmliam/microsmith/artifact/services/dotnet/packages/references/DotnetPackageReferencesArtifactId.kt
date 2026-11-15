package io.github.lmliam.microsmith.artifact.services.dotnet.packages.references

import io.github.lmliam.microsmith.artifact.ArtifactId
import kotlin.reflect.KClass

public data class DotnetPackageReferencesArtifactId(public val serviceName: String) :
    ArtifactId<DotnetPackageReferencesArtifact> {
    override val artifactType: KClass<DotnetPackageReferencesArtifact> = DotnetPackageReferencesArtifact::class
}
