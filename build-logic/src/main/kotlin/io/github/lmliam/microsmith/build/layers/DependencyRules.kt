package io.github.lmliam.microsmith.build.layers

private const val RUNTIME_SCRIPTING = "runtime-scripting"

internal fun dependencyViolations(projectDependencies: Map<String, Collection<String>>): List<ModuleLayerViolation> =
    projectDependencies.toSortedMap().flatMap { (module, dependencies) ->
        val moduleLayer = pipelineLayerOf(module) ?: return@flatMap emptyList()
        dependencies.sorted().mapNotNull { dependency -> dependencyViolation(module, moduleLayer, dependency) }
    }

private fun dependencyViolation(
    module: String,
    moduleLayer: PipelineLayer,
    dependency: String,
): ModuleLayerViolation? {
    val dependencyLayer = pipelineLayerOf(dependency)
    return when {
        dependencyLayer != null && dependencyLayer > moduleLayer ->
            ModuleLayerViolation.UpwardDependency(module, moduleLayer, dependency, dependencyLayer)
        moduleLayer == PipelineLayer.PLUGIN_API && isModuleOrSubmodule(dependency, RUNTIME_SCRIPTING) ->
            ModuleLayerViolation.PluginApiDependsOnRuntimeScripting(module, dependency)
        moduleLayer == PipelineLayer.PLUGIN && dependencyLayer == PipelineLayer.GEN ->
            ModuleLayerViolation.PluginDependsOnGen(module, dependency)
        else -> null
    }
}
