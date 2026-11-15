package io.github.lmliam.microsmith.build.layers

internal enum class PipelineLayer(
    val moduleName: String,
) {
    DSL("dsl"),
    RESOLVE("resolve"),
    ARTIFACT("artifact"),
    COMPILE("compile"),
    PLUGIN_API("plugin-api"),
    GEN("gen"),
    PLUGIN("plugin"),
    ;

    fun owns(module: String): Boolean = isModuleOrSubmodule(module, moduleName)
}

internal fun pipelineLayerOf(module: String): PipelineLayer? = PipelineLayer.entries.firstOrNull { it.owns(module) }

internal fun isModuleOrSubmodule(
    module: String,
    root: String,
): Boolean = module == root || module.startsWith("$root-")
