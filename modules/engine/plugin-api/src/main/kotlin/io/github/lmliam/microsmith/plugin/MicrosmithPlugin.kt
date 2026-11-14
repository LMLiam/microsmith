package io.github.lmliam.microsmith.plugin

/**
 * Entry point that a plugin jar registers with `ServiceLoader`.
 *
 * The host calls [services] once for each generation run and may call the returned services from several threads at
 * once within that run. Plugin services must be stateless or thread-safe.
 */
interface MicrosmithPlugin {
    val id: String

    fun services(host: HostServices): PluginServices
}
