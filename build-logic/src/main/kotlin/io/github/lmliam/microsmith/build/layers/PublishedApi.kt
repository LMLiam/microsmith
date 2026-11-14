package io.github.lmliam.microsmith.build.layers

private val publishedModulesOutsideLayers = setOf("runtime-scripting-api")

internal fun isPublishedApiModule(module: String): Boolean {
    val layer = pipelineLayerOf(module)
    return (layer != null && layer != PipelineLayer.GEN) || module in publishedModulesOutsideLayers
}
