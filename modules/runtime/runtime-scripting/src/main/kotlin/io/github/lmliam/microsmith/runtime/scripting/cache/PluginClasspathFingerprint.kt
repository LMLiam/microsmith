package io.github.lmliam.microsmith.runtime.scripting.cache

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.runtime.scripting.composition.ScriptRunScope
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest

@Inject
@SingleIn(ScriptRunScope::class)
internal class PluginClasspathFingerprint(request: ScriptRunRequest, runtimeFingerprint: RuntimeClasspathFingerprint) {
    val value: String = runtimeFingerprint.calculate(request.pluginClasspath)
}
