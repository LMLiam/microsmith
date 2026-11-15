package io.github.lmliam.microsmith.artifact

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import kotlin.reflect.KClass

@ServiceContract
public interface ArtifactRenderer<A : Artifact> {
    public val artifactType: KClass<A>

    public fun render(artifact: A): GeneratedFile
}
