package io.github.lmliam.microsmith.dsl
import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.MicrosmithExtension

fun <T : MicrosmithExtension> MicrosmithBuilder.put(type: Class<T>, ext: T) = put(type.kotlin, ext)

inline fun <reified T : MicrosmithExtension> MicrosmithBuilder.put(ext: T) = put(T::class, ext)
