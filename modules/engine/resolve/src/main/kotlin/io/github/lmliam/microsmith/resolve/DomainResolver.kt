package io.github.lmliam.microsmith.resolve

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import kotlin.reflect.KClass

@ServiceContract
public interface DomainResolver<A : MicrosmithExtension, R : ResolvedModel> {
    public val authoringType: KClass<A>
    public val resolvedType: KClass<R>

    public fun resolve(authoring: A): DomainResolution<R>
}
