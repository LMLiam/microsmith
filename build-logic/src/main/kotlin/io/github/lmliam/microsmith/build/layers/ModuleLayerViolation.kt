package io.github.lmliam.microsmith.build.layers

internal sealed interface ModuleLayerViolation {
    data class UpwardDependency(
        val module: String,
        val moduleLayer: PipelineLayer,
        val dependency: String,
        val dependencyLayer: PipelineLayer,
    ) : ModuleLayerViolation

    data class PluginApiDependsOnRuntimeScripting(
        val module: String,
        val dependency: String,
    ) : ModuleLayerViolation

    data class PluginDependsOnGen(
        val module: String,
        val dependency: String,
    ) : ModuleLayerViolation

    data class MetroTypeInPublishedApi(
        val module: String,
        val type: JvmClassName,
    ) : ModuleLayerViolation
}
