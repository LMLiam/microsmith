package io.github.lmliam.microsmith.dsl.helpers

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension

fun <T : MicrosmithExtension> MicrosmithBuilder.put(type: Class<T>, ext: T) = put(type.kotlin, ext)

inline fun <reified T : MicrosmithExtension> MicrosmithBuilder.put(ext: T) = put(T::class, ext)
