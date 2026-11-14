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
    ;

    fun owns(module: String): Boolean = module == moduleName || module.startsWith("$moduleName-")
}

internal fun pipelineLayerOf(module: String): PipelineLayer? = PipelineLayer.entries.firstOrNull { it.owns(module) }

private val publishedApiModules = setOf("runtime-scripting-api")

private val metroType = Regex("dev[/.]zacsweers[/.]metro")

internal fun isPublishedApiModule(module: String): Boolean {
    val layer = pipelineLayerOf(module)
    return (layer != null && layer != PipelineLayer.GEN) || module in publishedApiModules
}

internal fun findModuleLayerViolations(
    projectDependencies: Map<String, Collection<String>>,
    abiDumps: Map<String, List<String>>,
): List<String> = layerDirectionViolations(projectDependencies) + metroAbiViolations(abiDumps)

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

private fun metroAbiViolations(abiDumps: Map<String, List<String>>): List<String> =
    abiDumps.toSortedMap().flatMap { (module, dump) ->
        if (!isPublishedApiModule(module)) return@flatMap emptyList()
        dump.filter(metroType::containsMatchIn).map { "$module exposes a Metro type: ${it.trim()}" }
    }
