package io.github.lmliam.microsmith.compile

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import kotlin.reflect.KClass

@ServiceContract
public interface ArtifactCompiler<A : Artifact> {
    public val artifactType: KClass<A>

    public fun compile(artifact: A): List<ArtifactContribution<Artifact>>
}
