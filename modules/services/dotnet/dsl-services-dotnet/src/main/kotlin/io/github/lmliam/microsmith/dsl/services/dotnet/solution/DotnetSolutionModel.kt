package io.github.lmliam.microsmith.dsl.services.dotnet.solution

import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.merge.mergeModelExtension
import kotlin.reflect.KClass

/** Immutable snapshot of future solution-scoped extensions. */
public class DotnetSolutionModel
internal constructor(private val extensions: Map<KClass<out MicrosmithExtension>, MicrosmithExtension>) {
    @Suppress("UNCHECKED_CAST") public fun <T : MicrosmithExtension> get(type: KClass<T>): T? = extensions[type] as? T

    public inline fun <reified T : MicrosmithExtension> get(): T? = get(T::class)

    @Suppress("UNCHECKED_CAST")
    internal fun <T : MicrosmithExtension> with(type: KClass<T>, value: T) =
        DotnetSolutionModel(extensions + (mapOf(type to mergeModelExtension(extensions[type] as T?, value))))

    internal fun merge(other: DotnetSolutionModel): DotnetSolutionModel =
        DotnetSolutionModel(
            extensions + other.extensions.mapValues { (type, value) -> mergeModelExtension(extensions[type], value) },
        )

    internal fun keys() = extensions.keys

    override fun equals(other: Any?): Boolean = other is DotnetSolutionModel && extensions == other.extensions

    override fun hashCode(): Int = extensions.hashCode()

    public companion object {
        public fun empty(): DotnetSolutionModel = DotnetSolutionModel(emptyMap())
    }
}
