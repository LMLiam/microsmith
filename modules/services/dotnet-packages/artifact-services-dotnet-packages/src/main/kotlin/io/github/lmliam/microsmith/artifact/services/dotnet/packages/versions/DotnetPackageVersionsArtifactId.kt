package io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions

import io.github.lmliam.microsmith.artifact.ArtifactId
import kotlin.reflect.KClass

public data class DotnetPackageVersionsArtifactId(public val solutionName: String) :
    ArtifactId<DotnetPackageVersionsArtifact> {
    override val artifactType: KClass<DotnetPackageVersionsArtifact> = DotnetPackageVersionsArtifact::class
}
