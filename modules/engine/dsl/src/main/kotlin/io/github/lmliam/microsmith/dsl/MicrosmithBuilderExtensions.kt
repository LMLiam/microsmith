package io.github.lmliam.microsmith.dsl

import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.MicrosmithExtension

public fun <T : MicrosmithExtension> MicrosmithBuilder.put(type: Class<T>, ext: T): Unit = put(type.kotlin, ext)

public inline fun <reified T : MicrosmithExtension> MicrosmithBuilder.put(ext: T): Unit = put(T::class, ext)
