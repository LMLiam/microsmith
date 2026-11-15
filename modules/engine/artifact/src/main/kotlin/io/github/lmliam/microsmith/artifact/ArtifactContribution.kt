package io.github.lmliam.microsmith.artifact

public interface ArtifactContribution<out A : Artifact> {
    public val artifactId: ArtifactId<A>
}
