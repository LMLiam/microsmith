package io.github.lmliam.microsmith.runtime.scripting.host.execution

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.runtime.scripting.composition.ScriptRunScope
import io.github.lmliam.microsmith.runtime.scripting.definition.MicrosmithScript
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import java.net.URLClassLoader

@Inject
@SingleIn(ScriptRunScope::class)
internal class ScriptRunClassLoader(request: ScriptRunRequest) : AutoCloseable {
    private val hostLoader: ClassLoader = MicrosmithScript::class.java.classLoader
    val pluginLoader: URLClassLoader? =
        request.pluginClasspath
            .takeIf { it.isNotEmpty() }
            ?.let { classpath -> URLClassLoader(classpath.map { it.toUri().toURL() }.toTypedArray(), hostLoader) }

    val loader: ClassLoader = pluginLoader ?: hostLoader

    override fun close() {
        pluginLoader?.close()
    }
}
