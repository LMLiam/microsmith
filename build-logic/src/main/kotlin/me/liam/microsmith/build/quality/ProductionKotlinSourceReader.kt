package me.liam.microsmith.build.quality

import java.nio.file.Files
import java.nio.file.Path

internal object ProductionKotlinSourceReader {
    fun read(repositoryRoot: Path, sourceFile: Path): ProductionKotlinSource {
        val absolutePath = sourceFile.toAbsolutePath().normalize()
        val relativePath = repositoryRoot.relativize(absolutePath).toNormalizedPathString()
        val lines = Files.readAllLines(absolutePath)
        val topLevelLines = TopLevelKotlinLineScanner.scan(lines)
        return ProductionKotlinSource(
            path = absolutePath,
            relativePath = relativePath,
            sourceRootRelativePath = relativePath.toSourceRootRelativePath(),
            lineCount = lines.size,
            packageName = lines.firstNotNullOfOrNull(String::packageNameOrNull),
            topLevelProductionDeclarationCount = topLevelLines.count(String::isTopLevelProductionDeclarationLine),
            topLevelProductionDeclarationNames = topLevelLines.mapNotNull(String::topLevelProductionDeclarationNameOrNull),
        )
    }

    private fun Path.toNormalizedPathString(): String = toString().replace('\\', '/')

    private fun String.toSourceRootRelativePath(): String = when {
        startsWith(sourceRootRelativePrefix) -> removePrefix(sourceRootRelativePrefix)
        contains(sourceRootRelativeMarker) -> substringAfter(sourceRootRelativeMarker)
        else -> this
    }

    private const val sourceRootRelativePrefix = "src/main/kotlin/"
    private const val sourceRootRelativeMarker = "/src/main/kotlin/"
}
