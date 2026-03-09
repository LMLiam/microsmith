package me.liam.microsmith.build.quality

import java.nio.file.Path

internal data class ProductionKotlinSource(
    val path: Path,
    val relativePath: String,
    val sourceRootRelativePath: String,
    val lines: List<String>,
    val packageName: String?,
    val topLevelProductionDeclarationNames: List<String>,
) {
    val lineCount: Int = lines.size
    val sourceRootRelativeDirectory: String =
        sourceRootRelativePath.substringBeforeLast('/', missingDelimiterValue = "")
    val fileNameWithoutExtension: String = path.fileName.toString().removeSuffix(".kt")
}
