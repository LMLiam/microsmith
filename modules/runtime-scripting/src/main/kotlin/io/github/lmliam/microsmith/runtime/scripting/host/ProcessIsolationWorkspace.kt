package io.github.lmliam.microsmith.runtime.scripting.host

import java.nio.file.Path

internal data class ProcessIsolationWorkspace(val workingDirectory: Path, val requestFile: Path, val resultFile: Path)
