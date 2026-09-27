package io.github.lmliam.microsmith.artifact
interface ArtifactContribution<A : Artifact> {
    val artifactId: ArtifactId<A>
}
