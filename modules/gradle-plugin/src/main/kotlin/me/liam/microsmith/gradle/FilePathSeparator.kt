package me.liam.microsmith.gradle

internal object FilePathSeparator {
    val value: String = System.getProperty("path.separator") ?: ":"
}
