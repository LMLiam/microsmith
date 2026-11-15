package io.github.lmliam.microsmith.plugin

import java.nio.file.FileSystem
import kotlin.time.Clock

public interface HostServices {
    public val fileSystem: FileSystem
    public val clock: Clock
    public val diagnostics: DiagnosticSink
    public val console: Console
    public val environment: Environment
}
