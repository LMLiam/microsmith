package io.github.lmliam.microsmith.artifact
import kotlin.reflect.KClass

interface ArtifactId<A : Artifact> {
    val artifactType: KClass<A>
}
