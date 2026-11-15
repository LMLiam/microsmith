package thirdparty.plugins

import io.github.lmliam.microsmith.runtime.scripting.api.MicrosmithScriptingPlugin
import thirdparty.common.TopicContributor

class SymbolsScriptingPlugin : MicrosmithScriptingPlugin {
    override val id = "thirdparty.symbols"

    override val scriptSymbolContributors = setOf(TopicContributor())
}
