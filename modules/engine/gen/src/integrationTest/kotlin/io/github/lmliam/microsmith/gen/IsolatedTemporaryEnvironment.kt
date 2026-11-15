package io.github.lmliam.microsmith.gen

import io.github.lmliam.microsmith.plugin.Environment
import java.nio.file.Path

class IsolatedTemporaryEnvironment(
    private val temporaryDirectory: Path,
    private val onTemporaryDirectoryRequested: () -> Unit = {},
) : Environment {
    override fun variable(name: String): String? = System.getenv(name)

    override fun property(name: String): String? =
        if (name == "java.io.tmpdir") {
            onTemporaryDirectoryRequested()
            temporaryDirectory.toString()
        } else {
            System.getProperty(name)
        }
}
