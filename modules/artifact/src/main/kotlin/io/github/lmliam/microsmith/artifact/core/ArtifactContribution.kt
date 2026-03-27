package io.github.lmliam.microsmith.artifact.core

interface ArtifactContribution<A : Artifact> {
    val artifactId: ArtifactId<A>
}
