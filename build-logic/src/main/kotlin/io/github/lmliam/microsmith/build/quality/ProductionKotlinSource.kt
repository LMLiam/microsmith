package io.github.lmliam.microsmith.build.quality

import java.nio.file.Path

internal data class ProductionKotlinSource(
    val path: Path,
    val relativePath: String,
    val sourceRootRelativePath: String,
    val lineCount: Int,
    val packageName: String?,
    val topLevelProductionDeclarationCount: Int,
    val topLevelProductionDeclarationNames: List<String>,
) {
    val sourceRootRelativeDirectory: String =
        sourceRootRelativePath.substringBeforeLast('/', missingDelimiterValue = "")
    val fileNameWithoutExtension: String = path.fileName.toString().removeSuffix(".kt")
}
