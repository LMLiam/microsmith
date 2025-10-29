package me.liam.microsmith.gen.schemas

import me.liam.microsmith.dsl.schemas.core.Schema
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import kotlin.reflect.KClass

interface SchemaEmitter<T : Schema> {
    val type: KClass<T>

    suspend fun T.emit(space: FileSpace): GeneratedFile
}