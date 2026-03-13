package io.github.lmliam.microsmith.gradle

internal object FilePathSeparator {
    val value: String = System.getProperty("path.separator") ?: ":"
}
