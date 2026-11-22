package thirdparty.plugins

import io.github.lmliam.microsmith.runtime.scripting.api.MicrosmithScriptingPlugin
import kotlin.concurrent.thread
import thirdparty.common.ObservedTopicContributor
import thirdparty.contract.CollectabilityProbe
import thirdparty.contract.STUBBORN_THREAD_NAME

class StubbornThreadScriptingPlugin : MicrosmithScriptingPlugin {
    override val id = "thirdparty.stubborn-thread"

    override val scriptSymbolContributors =
        setOf(
            ObservedTopicContributor { contributor ->
                CollectabilityProbe.track(contributor)
                thread(isDaemon = true, name = STUBBORN_THREAD_NAME) {
                    while (!CollectabilityProbe.stopRequested) {
                        runCatching { Thread.sleep(STUBBORN_POLL_MILLIS) }
                    }
                }
            },
        )
}

private const val STUBBORN_POLL_MILLIS = 20L
