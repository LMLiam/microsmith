package io.github.lmliam.microsmith.artifact.services.dotnet.packages

import io.github.lmliam.microsmith.artifact.core.ArtifactId
import kotlin.reflect.KClass

data class DotnetPackageVersionsArtifactId(
    val solutionName: String,
) : ArtifactId<DotnetPackageVersionsArtifact> {
    override val artifactType: KClass<DotnetPackageVersionsArtifact> = DotnetPackageVersionsArtifact::class
}
