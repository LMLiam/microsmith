package thirdparty.plugins

import io.github.lmliam.microsmith.runtime.scripting.api.MicrosmithScriptingPlugin
import thirdparty.common.ObservedTopicContributor
import thirdparty.contract.CollectabilityProbe

class LeakScriptingPlugin : MicrosmithScriptingPlugin {
    override val id = "thirdparty.leak-scripting"

    override val scriptSymbolContributors = setOf(ObservedTopicContributor(CollectabilityProbe::track))
}
