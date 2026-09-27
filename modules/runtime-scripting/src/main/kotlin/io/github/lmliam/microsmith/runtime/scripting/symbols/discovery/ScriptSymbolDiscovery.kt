package io.github.lmliam.microsmith.runtime.scripting.symbols.discovery
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallCallee
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallSite
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolContributor
import io.github.lmliam.microsmith.runtime.scripting.symbols.model.DiscoveredScriptSymbol
import io.github.lmliam.microsmith.runtime.scripting.symbols.parsing.KotlinScriptCallSiteParser

internal object ScriptSymbolDiscovery {
    fun discover(
        sourceName: String,
        sourceText: String,
        registry: ScriptSymbolContributorRegistry,
    ): List<DiscoveredScriptSymbol> {
        val contributorsByCallName = registry.contributors
            .flatMap { contributor ->
                contributor.declarationCallNames
                    .map { callName -> callName to contributor }
            }
            .groupBy(
                keySelector = Pair<String, ScriptSymbolContributor>::first,
                valueTransform = Pair<String, ScriptSymbolContributor>::second,
            )

        val sites = KotlinScriptCallSiteParser.declarations(
            sourceName = sourceName,
            sourceText = sourceText,
            declarationCallNames = contributorsByCallName.keys,
        )

        val discovered = sites.flatMap { site ->
            val callName = site.call.namedCallName() ?: return@flatMap emptyList()

            contributorsByCallName[callName]
                .orEmpty()
                .mapNotNull { contributor ->
                    contributor
                        .discover(site)
                        ?.takeIf { definition -> definition.propertyName.isAutomaticSymbolIdentifier() }
                        ?.let { definition ->
                            DiscoveredScriptSymbol(
                                contributor,
                                definition,
                            )
                        }
                }
        }

        return requireUnambiguousSymbols(discovered)
    }

    private fun requireUnambiguousSymbols(symbols: List<DiscoveredScriptSymbol>): List<DiscoveredScriptSymbol> = symbols
        .groupBy { it.definition.propertyName }
        .map { (propertyName, registrations) ->
            val distinct = registrations.distinctBy {
                listOf(
                    it.contributor.id,
                    it.definition.valueType.qualifiedName,
                    it.definition.kind,
                    it.definition.valueKey,
                )
            }

            require(distinct.size == 1) {
                val targets = distinct
                    .map { "${it.contributor.id}: ${it.definition.valueKey}" }
                    .sorted()
                    .joinToString(", ")

                "Automatic script symbol '$propertyName' is ambiguous between: $targets"
            }

            distinct.single()
        }
        .sortedBy { it.definition.propertyName }
}

private fun ScriptCallSite.namedCallName(): String? = (callee as? ScriptCallCallee.Named)?.name

private fun String.isAutomaticSymbolIdentifier(): Boolean = matches(AUTOMATIC_SYMBOL_IDENTIFIER)

private val AUTOMATIC_SYMBOL_IDENTIFIER =
    Regex("[A-Za-z_][A-Za-z0-9_]*")
