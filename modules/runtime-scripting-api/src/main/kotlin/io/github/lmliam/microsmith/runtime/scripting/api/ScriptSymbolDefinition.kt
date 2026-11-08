package io.github.lmliam.microsmith.runtime.scripting.api
import kotlin.reflect.KClass

data class ScriptSymbolDefinition(
    val propertyName: String,
    val valueType: KClass<*>,
    val kind: String,
    val valueKey: String,
)
