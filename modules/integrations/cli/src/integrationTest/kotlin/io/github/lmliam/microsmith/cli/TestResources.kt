package io.github.lmliam.microsmith.cli

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

private object TestResources

internal fun copyTestResource(resource: String, target: Path) {
    val source = checkNotNull(TestResources::class.java.getResource(resource)) { "Missing test resource $resource" }
    target.parent?.createDirectories()
    target.writeText(source.readText())
}
