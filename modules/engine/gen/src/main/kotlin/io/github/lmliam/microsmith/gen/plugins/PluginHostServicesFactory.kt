package io.github.lmliam.microsmith.gen.plugins

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.plugin.Console
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import io.github.lmliam.microsmith.plugin.Environment
import io.github.lmliam.microsmith.plugin.HostServices
import java.nio.file.FileSystem
import kotlin.time.Clock

@Inject
internal class PluginHostServicesFactory(
    private val fileSystem: FileSystem,
    private val clock: Clock,
    private val diagnostics: DiagnosticSink,
    private val console: Console,
    private val environment: Environment,
) {
    fun create(pluginId: String): HostServices =
        object : HostServices {
            override val fileSystem = this@PluginHostServicesFactory.fileSystem
            override val clock = this@PluginHostServicesFactory.clock
            override val diagnostics: DiagnosticSink =
                PluginDiagnosticSink(pluginId, this@PluginHostServicesFactory.diagnostics)
            override val console = this@PluginHostServicesFactory.console
            override val environment = this@PluginHostServicesFactory.environment
        }
}
