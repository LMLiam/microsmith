package io.github.lmliam.microsmith.plugin

import java.nio.file.FileSystem
import kotlin.time.Clock

interface HostServices {
    val fileSystem: FileSystem
    val clock: Clock
    val diagnostics: DiagnosticSink
    val console: Console
    val environment: Environment
}
