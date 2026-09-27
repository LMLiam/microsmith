package io.github.lmliam.microsmith.runtime.scripting.symbols.discovery

import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolContributor
import java.util.ServiceLoader

internal class ScriptSymbolContributorRegistry private constructor(val contributors: List<ScriptSymbolContributor>) {
    private val contributorsById = contributors.associateBy(ScriptSymbolContributor::id)

    fun contributor(id: String): ScriptSymbolContributor = contributorsById[id]
        ?: error("Script symbol contributor with '$id' is not available during evaluation")

    companion object {
        fun discover(classLoader: ClassLoader = defaultClassLoader()): ScriptSymbolContributorRegistry = of(
            buildList {
                add(ProtobufScriptSymbolContributor)
                addAll(
                    ServiceLoader.load(ScriptSymbolContributor::class.java, classLoader),
                )
            },
        )

        fun of(contributors: Iterable<ScriptSymbolContributor>): ScriptSymbolContributorRegistry {
            val registrations = contributors.toList()

            val duplicateIds = registrations
                .groupingBy(ScriptSymbolContributor::id)
                .eachCount()
                .filterValues { it > 1 }
                .keys
                .sorted()

            require(duplicateIds.isEmpty()) {
                "Duplicate script symbol contributor ids: ${duplicateIds.joinToString(", ")}"
            }

            return ScriptSymbolContributorRegistry(registrations.sortedBy(ScriptSymbolContributor::id))
        }

        private fun defaultClassLoader(): ClassLoader = Thread.currentThread().contextClassLoader
            ?: ScriptSymbolContributorRegistry::class.java.classLoader
    }
}
