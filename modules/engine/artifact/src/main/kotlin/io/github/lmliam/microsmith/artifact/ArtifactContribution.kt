package io.github.lmliam.microsmith.artifact

interface ArtifactContribution<out A : Artifact> {
    val artifactId: ArtifactId<A>
}
