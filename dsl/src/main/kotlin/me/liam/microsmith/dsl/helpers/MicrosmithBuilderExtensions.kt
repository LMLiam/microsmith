package me.liam.microsmith.dsl.helpers

import me.liam.microsmith.dsl.core.MicrosmithBuilder
import me.liam.microsmith.dsl.core.MicrosmithExtension

fun <T : MicrosmithExtension> MicrosmithBuilder.put(
    type: Class<T>,
    ext: T
) = put(type.kotlin, ext)

inline fun <reified T : MicrosmithExtension> MicrosmithBuilder.put(ext: T) = put(T::class, ext)