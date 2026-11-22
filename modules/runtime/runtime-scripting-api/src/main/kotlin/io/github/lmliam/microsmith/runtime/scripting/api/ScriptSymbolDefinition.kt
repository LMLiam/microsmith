package io.github.lmliam.microsmith.runtime.scripting.api

import kotlin.reflect.KClass

public data class ScriptSymbolDefinition(
    public val propertyName: String,
    public val valueType: KClass<*>,
    public val kind: String,
    public val valueKey: String,
    public val qualifiedPath: List<String> = listOf(propertyName),
)
