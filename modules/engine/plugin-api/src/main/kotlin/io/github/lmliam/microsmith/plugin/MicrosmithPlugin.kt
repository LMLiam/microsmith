package io.github.lmliam.microsmith.plugin

/**
 * Entry point that a plugin jar registers with `ServiceLoader`.
 *
 * The host calls [services] once for each generation run and may call the returned services from several threads at
 * once within that run. Plugin services must be stateless or thread-safe.
 */
public interface MicrosmithPlugin {
    public val id: String

    public fun services(host: HostServices): PluginServices
}
