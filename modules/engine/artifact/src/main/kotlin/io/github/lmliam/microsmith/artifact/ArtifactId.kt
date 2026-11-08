package io.github.lmliam.microsmith.artifact

import kotlin.reflect.KClass

interface ArtifactId<out A : Artifact> {
    val artifactType: KClass<out A>
}
