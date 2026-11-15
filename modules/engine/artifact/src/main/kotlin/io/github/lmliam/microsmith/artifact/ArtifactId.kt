package io.github.lmliam.microsmith.artifact

import kotlin.reflect.KClass

public interface ArtifactId<out A : Artifact> {
    public val artifactType: KClass<out A>
}
