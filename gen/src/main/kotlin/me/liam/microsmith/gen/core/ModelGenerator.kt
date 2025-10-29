package me.liam.microsmith.gen.core

import me.liam.microsmith.dsl.core.MicrosmithExtension
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile

interface ModelGenerator<T : MicrosmithExtension> {
    suspend fun T.generate(space: FileSpace): List<GeneratedFile>
}