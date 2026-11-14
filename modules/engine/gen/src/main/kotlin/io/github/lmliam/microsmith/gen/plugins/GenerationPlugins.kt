package io.github.lmliam.microsmith.gen.plugins

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.gen.errors.PluginConflictException
import kotlin.reflect.KClass

internal class GenerationPlugins
private constructor(private val open: List<OpenPlugin>, val catalog: MicrosmithPluginCatalog) {
    fun close(failure: Throwable?) = closeAll(open, failure)

    companion object {
        @Suppress("TooGenericExceptionCaught")
        fun load(
            classLoader: ClassLoader,
            hostServices: PluginHostServicesFactory,
            builtIns: MicrosmithPluginCatalog,
        ): GenerationPlugins {
            val legacy = discoverMicrosmithPlugins(classLoader)
            val open = mutableListOf<OpenPlugin>()

            try {
                discoverPlugins(classLoader).mapTo(open) { OpenPlugin.open(it, hostServices) }

                val sources =
                    mapOf("the host" to builtIns, "service providers" to legacy) +
                        open.associate { "plugin '${it.id}'" to it.catalog() }
                requireOneServicePerArtifactType(sources)

                return GenerationPlugins(open, sources.values.reduce { all, next -> all + next })
            } catch (failure: Throwable) {
                closeAll(open, failure)
                throw failure
            }
        }

        internal fun requireOneServicePerArtifactType(sources: Map<String, MicrosmithPluginCatalog>) {
            requireOneServicePerArtifactType("compiler", sources, { it.artifactCompilers }) { it.artifactType }
            requireOneServicePerArtifactType("assembler", sources, { it.artifactAssemblers }) { it.artifactType }
            requireOneServicePerArtifactType("renderer", sources, { it.artifactRenderers }) { it.artifactType }
        }

        private fun closeAll(plugins: List<OpenPlugin>, failure: Throwable?) {
            plugins.asReversed().forEach { plugin ->
                val closeFailure = plugin.close()

                if (failure != null && closeFailure != null) failure.addSuppressed(closeFailure)
            }
        }

        private fun <T : Any> requireOneServicePerArtifactType(
            kind: String,
            sources: Map<String, MicrosmithPluginCatalog>,
            servicesOf: (MicrosmithPluginCatalog) -> List<T>,
            artifactTypeOf: (T) -> KClass<out Artifact>,
        ) {
            val conflicts =
                sources.entries
                    .flatMap { (label, catalog) -> servicesOf(catalog).map { service -> label to service } }
                    .groupBy { (_, service) -> artifactTypeOf(service).java.name }
                    .filterValues { it.size > 1 }
                    .toSortedMap()
                    .map { (type, claimants) ->
                        "artifact type '$type' has a $kind from " +
                            claimants
                                .map { (label, service) -> "$label (${service::class.java.name})" }
                                .sorted()
                                .joinToString(" and ")
                    }

            if (conflicts.isNotEmpty())
                throw PluginConflictException("Conflicting ${kind}s: ${conflicts.joinToString("; ")}.")
        }
    }
}
