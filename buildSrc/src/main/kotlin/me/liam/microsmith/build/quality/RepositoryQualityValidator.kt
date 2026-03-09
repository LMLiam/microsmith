package me.liam.microsmith.build.quality

import java.nio.file.Files
import java.nio.file.Path

internal class RepositoryQualityValidator(
    private val policy: RepositoryQualityPolicy,
) {
    fun validate(repositoryRoot: Path, sourceFiles: Iterable<Path>): List<RepositoryQualityViolation> =
        sourceFiles
            .map { sourceFile -> sourceFile.toProductionKotlinSource(repositoryRoot) }
            .flatMap { source ->
                listOfNotNull(
                    validateTopLevelProductionDeclarations(source),
                    validateLineCount(source),
                    validatePackageSegments(source),
                )
            }
            .sortedWith(compareBy(RepositoryQualityViolation::rule, RepositoryQualityViolation::path))

    private fun validateTopLevelProductionDeclarations(source: ProductionKotlinSource): RepositoryQualityViolation? {
        if (policy.multiDeclarationExemptionFor(source.relativePath) != null) {
            return null
        }

        val declarationCount = source.lines.count { line -> line.isTopLevelProductionDeclaration() }
        if (declarationCount <= 1) {
            return null
        }

        return RepositoryQualityViolation(
            rule = "multiple-production-types",
            path = source.relativePath,
            message = "$declarationCount non-private top-level production declarations found. Split extra production types into their own files or make tightly coupled helpers private.",
        )
    }

    private fun validateLineCount(source: ProductionKotlinSource): RepositoryQualityViolation? {
        val maxLines = policy.maxLinesFor(source.relativePath)
        if (source.lineCount <= maxLines) {
            return null
        }

        val override = policy.productionFileLineOverrides[source.relativePath]
        val rationale = override?.rationale?.let { " Override rationale: $it" }.orEmpty()
        return RepositoryQualityViolation(
            rule = "production-file-lines",
            path = source.relativePath,
            message = "${source.lineCount} lines exceeds the allowed maximum of $maxLines. Split parsing, validation, rendering, diagnostics, policy, or I/O responsibilities before extending the file further.$rationale",
        )
    }

    private fun validatePackageSegments(source: ProductionKotlinSource): RepositoryQualityViolation? {
        val packageName = source.packageName ?: return null
        val forbiddenSegment = packageName.split('.').firstOrNull(policy.forbiddenPackageSegments::contains) ?: return null

        return RepositoryQualityViolation(
            rule = "forbidden-package-segment",
            path = source.relativePath,
            message = "Package '$packageName' contains forbidden segment '$forbiddenSegment'. Use a domain-led package name instead of util/utils/misc.",
        )
    }

    private fun Path.toProductionKotlinSource(repositoryRoot: Path): ProductionKotlinSource {
        val absolutePath = toAbsolutePath().normalize()
        val relativePath = repositoryRoot.relativize(absolutePath).toNormalizedPathString()
        val lines = Files.readAllLines(absolutePath)
        return ProductionKotlinSource(
            path = absolutePath,
            relativePath = relativePath,
            lines = lines,
            packageName = lines.firstNotNullOfOrNull { line -> line.packageNameOrNull() },
        )
    }

    private fun Path.toNormalizedPathString(): String = toString().replace('\\', '/')

    private fun String.packageNameOrNull(): String? = packageDeclarationPattern.matchEntire(trim())?.groupValues?.get(1)

    private fun String.isTopLevelProductionDeclaration(): Boolean {
        val line = trimEnd()
        if (line.isBlank() || line.first().isWhitespace() || line.startsWith("private ")) {
            return false
        }

        return topLevelTypeDeclarationPattern.containsMatchIn(line) ||
            topLevelFunInterfacePattern.containsMatchIn(line) ||
            topLevelTypeAliasPattern.containsMatchIn(line)
    }

    private companion object {
        private val packageDeclarationPattern = Regex("^package\\s+([A-Za-z0-9_.]+)$")
        private val topLevelTypeDeclarationPattern = Regex(
            "^(?:(?:public|internal)\\s+)?(?:(?:data|sealed|value|enum|annotation)\\s+)*(?:class|interface|object)\\b",
        )
        private val topLevelFunInterfacePattern = Regex("^(?:(?:public|internal)\\s+)?fun\\s+interface\\b")
        private val topLevelTypeAliasPattern = Regex("^(?:(?:public|internal)\\s+)?typealias\\b")
    }
}
