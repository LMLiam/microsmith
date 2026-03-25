package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.core.ArtifactId
import kotlin.reflect.KClass

data class DotnetPackageReferencesArtifactId(
    val serviceName: String,
) : ArtifactId<DotnetPackageReferencesArtifact> {
    override val artifactType: KClass<DotnetPackageReferencesArtifact> = DotnetPackageReferencesArtifact::class
}
