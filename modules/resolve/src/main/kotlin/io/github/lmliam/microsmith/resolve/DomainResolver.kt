package io.github.lmliam.microsmith.resolve
import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import kotlin.reflect.KClass

@ServiceContract
interface DomainResolver<A : MicrosmithExtension, R : ResolvedModel> {
    val authoringType: KClass<A>
    val resolvedType: KClass<R>

    fun resolve(authoring: A): DomainResolution<R>
}
