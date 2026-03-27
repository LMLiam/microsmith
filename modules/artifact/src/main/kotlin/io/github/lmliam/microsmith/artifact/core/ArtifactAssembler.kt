package io.github.lmliam.microsmith.artifact.core

import com.github.eventhorizonlab.spi.ServiceContract
import kotlin.reflect.KClass

@ServiceContract
interface ArtifactAssembler<A : Artifact> {
    val artifactType: KClass<A>

    fun create(first: ArtifactContribution<A>): A

    fun merge(current: A, contribution: ArtifactContribution<A>): A
}
