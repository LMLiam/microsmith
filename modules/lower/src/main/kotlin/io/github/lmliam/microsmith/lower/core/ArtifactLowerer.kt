package io.github.lmliam.microsmith.lower.core

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import kotlin.reflect.KClass

@ServiceContract
interface ArtifactLowerer<A : Artifact> {
    val artifactType: KClass<A>

    fun lower(artifact: A): List<ArtifactContribution<out Artifact>>
}
