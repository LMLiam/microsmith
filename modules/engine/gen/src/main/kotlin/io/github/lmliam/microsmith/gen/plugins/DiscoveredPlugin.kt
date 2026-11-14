package io.github.lmliam.microsmith.gen.plugins

import io.github.lmliam.microsmith.gen.errors.PluginConflictException
import io.github.lmliam.microsmith.gen.errors.PluginLoadFailedException
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

internal class DiscoveredPlugin(val plugin: MicrosmithPlugin, val id: String) {
    val description: String
        get() = "plugin '$id' (${providerDescription(plugin)})"
}

internal fun discoverPlugins(classLoader: ClassLoader): List<DiscoveredPlugin> {
    val providers =
        loadingPlugin({ "Plugin discovery" }, "reading the service registrations") {
            ServiceLoader.load(MicrosmithPlugin::class.java, classLoader).stream().toList()
        }
    val discovered =
        providers
            .map { provider ->
                loadingPlugin({ "Plugin provider '${provider.type().name}'" }, "being instantiated") { provider.get() }
            }
            .map { plugin ->
                loadingPlugin({ providerDescription(plugin) }, "reading its id") { DiscoveredPlugin(plugin, plugin.id) }
            }
            .sortedBy { it.id }

    requireDistinctIds(discovered)

    return discovered
}

internal fun failureSummary(failure: Throwable): String = "${failure::class.java.name}: ${failure.message}"

@Suppress("TooGenericExceptionCaught")
internal inline fun <T> loadingPlugin(subject: () -> String, stage: String, block: () -> T): T =
    try {
        block()
    } catch (failure: Exception) {
        throw loadFailure(subject(), stage, failure)
    } catch (failure: LinkageError) {
        throw loadFailure(subject(), stage, failure)
    } catch (failure: ServiceConfigurationError) {
        throw loadFailure(subject(), stage, failure)
    }

internal fun loadFailure(subject: String, stage: String, failure: Throwable) =
    PluginLoadFailedException(
        if (failure is LinkageError) {
            "$subject is incompatible with this version of microsmith: ${failureSummary(failure)}"
        } else {
            "$subject failed while $stage: ${failureSummary(failure)}"
        },
        failure,
    )

private fun providerDescription(provider: Any): String {
    val className = provider::class.java.name
    val location = provider::class.java.protectionDomain?.codeSource?.location

    return if (location == null) "provider '$className'" else "provider '$className' in '$location'"
}

private fun requireDistinctIds(plugins: List<DiscoveredPlugin>) {
    val duplicates = plugins.groupBy { it.id }.filterValues { it.size > 1 }

    if (duplicates.isNotEmpty()) {
        throw PluginConflictException(
            "Duplicate plugin ids: " +
                duplicates.entries.joinToString("; ") { (id, claimants) ->
                    "'$id' is declared by " +
                        claimants.map { providerDescription(it.plugin) }.sorted().joinToString(" and ")
                } +
                ".",
        )
    }
}
