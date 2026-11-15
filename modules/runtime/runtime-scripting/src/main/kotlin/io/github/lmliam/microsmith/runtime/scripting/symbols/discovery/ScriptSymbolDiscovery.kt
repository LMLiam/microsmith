package io.github.lmliam.microsmith.runtime.scripting.symbols.discovery

import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallCallee
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallSite
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolContributor
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolDefinition
import io.github.lmliam.microsmith.runtime.scripting.symbols.model.DiscoveredScriptSymbol
import io.github.lmliam.microsmith.runtime.scripting.symbols.parsing.KotlinScriptCallSiteParser

internal data class ScriptSymbolDiscoveryResult(
    val symbols: List<DiscoveredScriptSymbol>,
    val omittedPlainNames: Map<String, List<String>>,
)

internal object ScriptSymbolDiscovery {
    fun discover(
        sourceName: String,
        sourceText: String,
        registry: ScriptSymbolContributorRegistry,
    ): ScriptSymbolDiscoveryResult {
        val contributorsByCallName =
            registry.contributors
                .flatMap { contributor -> contributor.declarationCallNames.map { callName -> callName to contributor } }
                .groupBy(
                    keySelector = Pair<String, ScriptSymbolContributor>::first,
                    valueTransform = Pair<String, ScriptSymbolContributor>::second,
                )

        val sites =
            KotlinScriptCallSiteParser.declarations(
                sourceName = sourceName,
                sourceText = sourceText,
                declarationCallNames = contributorsByCallName.keys,
            )

        val declarations = sites.flatMap { site ->
            val callName = site.call.namedCallName() ?: return@flatMap emptyList()

            contributorsByCallName[callName].orEmpty().mapNotNull { contributor ->
                contributor.discover(site)?.let { definition -> Declaration(contributor, definition) }
            }
        }

        return resolveSymbols(declarations.distinctBy(Declaration::target))
    }

    private fun resolveSymbols(declarations: List<Declaration>): ScriptSymbolDiscoveryResult {
        val qualified =
            declarations.filter { it.qualifiedName.isAutomaticSymbolIdentifier() }.groupBy { it.qualifiedName }

        qualified.forEach { (name, group) ->
            require(group.size == 1) {
                "Automatic script symbol '$name' is ambiguous between: " +
                    group.map { "${it.contributor.id}: ${it.definition.valueKey}" }.sorted().joinToString(", ")
            }
        }

        val plain =
            declarations
                .filter { it.definition.propertyName.isAutomaticSymbolIdentifier() }
                .groupBy { it.definition.propertyName }

        val symbols =
            qualified.values.map { (declaration) -> declaration.symbol(declaration.qualifiedName) } +
                plain
                    .filter { (name, group) -> group.size == 1 && name !in qualified }
                    .map { (name, group) -> group.single().symbol(name) }

        return ScriptSymbolDiscoveryResult(
            symbols = symbols.sortedBy(DiscoveredScriptSymbol::propertyName),
            omittedPlainNames =
                plain.filterValues { it.size > 1 }.mapValues { (_, group) -> group.map { it.qualifiedName }.sorted() },
        )
    }

    private class Declaration(val contributor: ScriptSymbolContributor, val definition: ScriptSymbolDefinition) {
        val qualifiedName: String = definition.qualifiedPath.joinToString(QUALIFIED_NAME_SEPARATOR)

        val target: List<Any?> =
            listOf(contributor.id, definition.valueType.qualifiedName, definition.kind, definition.valueKey)

        fun symbol(propertyName: String) = DiscoveredScriptSymbol(contributor, definition, propertyName)
    }
}

private fun ScriptCallSite.namedCallName(): String? = (callee as? ScriptCallCallee.Named)?.name

private fun String.isAutomaticSymbolIdentifier(): Boolean = matches(AUTOMATIC_SYMBOL_IDENTIFIER)

private const val QUALIFIED_NAME_SEPARATOR = "_"

private val AUTOMATIC_SYMBOL_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
