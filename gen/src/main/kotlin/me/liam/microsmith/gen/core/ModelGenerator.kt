package me.liam.microsmith.gen.core

import me.liam.microsmith.dsl.core.MicrosmithExtension

interface ModelGenerator<T : MicrosmithExtension> {
    suspend fun T.generate()
}