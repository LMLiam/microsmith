package thirdparty.plugins

import io.github.lmliam.microsmith.runtime.scripting.api.MicrosmithScriptingPlugin
import thirdparty.common.ObservedTopicContributor
import thirdparty.contract.CollectabilityProbe

class ThreadLocalScriptingPlugin : MicrosmithScriptingPlugin {
    override val id = "thirdparty.thread-local"

    private val leaked = ThreadLocal<Any>()

    override val scriptSymbolContributors =
        setOf(
            ObservedTopicContributor { contributor ->
                CollectabilityProbe.track(contributor)
                leaked.set(contributor)
            },
        )
}
