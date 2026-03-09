package me.liam.microsmith.build.quality

import java.nio.file.Path

internal data class ProductionKotlinSource(
    val path: Path,
    val relativePath: String,
    val lines: List<String>,
    val packageName: String?,
) {
    val lineCount: Int = lines.size
}
