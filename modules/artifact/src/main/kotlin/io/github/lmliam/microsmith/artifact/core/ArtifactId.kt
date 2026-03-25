package io.github.lmliam.microsmith.artifact.core

import kotlin.reflect.KClass

interface ArtifactId<A : Artifact> {
    val artifactType: KClass<A>
}
