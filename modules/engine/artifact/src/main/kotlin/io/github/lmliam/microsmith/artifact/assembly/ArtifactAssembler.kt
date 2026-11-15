package io.github.lmliam.microsmith.artifact.assembly

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import kotlin.reflect.KClass

@ServiceContract
public interface ArtifactAssembler<A : Artifact> {
    public val artifactType: KClass<A>

    public fun create(contribution: ArtifactContribution<A>): A

    public fun merge(
        current: A,
        contribution: ArtifactContribution<A>,
    ): A
}
