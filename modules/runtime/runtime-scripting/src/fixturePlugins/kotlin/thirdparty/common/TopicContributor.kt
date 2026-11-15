package thirdparty.common

import io.github.lmliam.microsmith.runtime.scripting.api.ScriptLiteral
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolContributor
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolDeclarationSite
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolDefinition

open class TopicContributor : ScriptSymbolContributor {
    override val id = "thirdparty.topics"

    override val declarationCallNames = setOf("fixtureTopic")

    override fun discover(site: ScriptSymbolDeclarationSite): ScriptSymbolDefinition? {
        val name = (site.call.arguments.firstOrNull() as? ScriptLiteral.StringValue)?.value ?: return null

        return ScriptSymbolDefinition(
            propertyName = name,
            valueType = TopicRef::class,
            kind = "topic",
            valueKey = name,
        )
    }

    override fun createValue(kind: String, valueKey: String): Any = TopicRef(valueKey)
}
