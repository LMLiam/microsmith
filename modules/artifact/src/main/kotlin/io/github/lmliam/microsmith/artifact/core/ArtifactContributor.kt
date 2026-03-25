package io.github.lmliam.microsmith.artifact.core

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.resolve.core.ResolvedModel
import kotlin.reflect.KClass

@ServiceContract
interface ArtifactContributor<R : ResolvedModel> {
    val resolvedType: KClass<R>

    fun contribute(model: R): List<ArtifactContribution<out Artifact>>
}
