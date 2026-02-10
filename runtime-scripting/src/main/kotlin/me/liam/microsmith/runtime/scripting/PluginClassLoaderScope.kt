package me.liam.microsmith.runtime.scripting

import java.net.URLClassLoader
import java.nio.file.Path

internal object PluginClassLoaderScope {
    fun <T> withPluginClassLoader(
        pluginClasspath: List<Path>,
        block: (ClassLoader) -> T
    ): T {
        val parentClassLoader = MicrosmithScript::class.java.classLoader
        if (pluginClasspath.isEmpty()) {
            return withContextClassLoader(parentClassLoader) {
                block(parentClassLoader)
            }
        }

        val urls = pluginClasspath.map { it.toUri().toURL() }.toTypedArray()
        return URLClassLoader(urls, parentClassLoader).use { pluginClassLoader ->
            withContextClassLoader(pluginClassLoader) {
                block(pluginClassLoader)
            }
        }
    }

    private fun <T> withContextClassLoader(
        classLoader: ClassLoader,
        block: () -> T
    ): T {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = classLoader
        return try {
            block()
        } finally {
            thread.contextClassLoader = previous
        }
    }
}
