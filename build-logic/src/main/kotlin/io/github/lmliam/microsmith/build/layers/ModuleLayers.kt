package io.github.lmliam.microsmith.build.layers

internal enum class PipelineLayer(
    val moduleName: String,
) {
    DSL("dsl"),
    RESOLVE("resolve"),
    ARTIFACT("artifact"),
    COMPILE("compile"),
    GEN("gen"),
    ;

    fun owns(module: String): Boolean = module == moduleName || module.startsWith("$moduleName-")
}

internal fun pipelineLayerOf(module: String): PipelineLayer? = PipelineLayer.entries.firstOrNull { it.owns(module) }

internal fun findModuleLayerViolations(
    projectDependencies: Map<String, Collection<String>>,
    metroModules: Collection<String>,
): List<String> = layerDirectionViolations(projectDependencies) + metroViolations(metroModules)

private fun layerDirectionViolations(projectDependencies: Map<String, Collection<String>>): List<String> =
    projectDependencies.toSortedMap().flatMap { (module, dependencies) ->
        val moduleLayer = pipelineLayerOf(module) ?: return@flatMap emptyList()
        dependencies.sorted().mapNotNull { dependency ->
            val dependencyLayer = pipelineLayerOf(dependency)
            if (dependencyLayer != null && dependencyLayer > moduleLayer) {
                "$module (${moduleLayer.moduleName}) -> $dependency (${dependencyLayer.moduleName})"
            } else {
                null
            }
        }
    }

private fun metroViolations(metroModules: Collection<String>): List<String> =
    metroModules.filter { it != PipelineLayer.GEN.moduleName }.sorted().map { "$it applies Metro; only ${PipelineLayer.GEN.moduleName} may" }
