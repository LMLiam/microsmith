package io.github.lmliam.microsmith.gen.core

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import kotlin.reflect.KClass

@ServiceContract
interface ArtifactRenderer<A : Artifact> {
    val artifactType: KClass<A>

    fun render(artifact: A): GeneratedFile
}
