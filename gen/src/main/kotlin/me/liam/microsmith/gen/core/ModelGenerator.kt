package me.liam.microsmith.gen.core

import me.liam.microsmith.dsl.core.MicrosmithExtension
import me.liam.microsmith.gen.files.FileSpace
import java.nio.file.Path

interface ModelGenerator<T : MicrosmithExtension> {
    suspend fun T.generate(space: FileSpace): List<Path>
}