package io.github.lmliam.microsmith.compile

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import kotlin.reflect.KClass

@ServiceContract
interface ArtifactCompiler<A : Artifact> {
    val artifactType: KClass<A>

    fun compile(artifact: A): List<ArtifactContribution<out Artifact>>
}
