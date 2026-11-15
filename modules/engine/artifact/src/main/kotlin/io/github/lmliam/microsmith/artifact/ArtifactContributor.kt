package io.github.lmliam.microsmith.artifact

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.resolve.ResolvedModel
import kotlin.reflect.KClass

@ServiceContract
public interface ArtifactContributor<R : ResolvedModel> {
    public val resolvedType: KClass<R>

    public fun contribute(model: R): List<ArtifactContribution<Artifact>>
}
