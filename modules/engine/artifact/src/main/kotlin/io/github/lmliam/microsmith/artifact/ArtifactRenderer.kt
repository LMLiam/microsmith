package io.github.lmliam.microsmith.artifact

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import kotlin.reflect.KClass

@ServiceContract
interface ArtifactRenderer<A : Artifact> {
    val artifactType: KClass<A>

    fun render(artifact: A): GeneratedFile
}
