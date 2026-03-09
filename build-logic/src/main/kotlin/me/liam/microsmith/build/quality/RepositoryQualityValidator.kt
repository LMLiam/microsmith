package me.liam.microsmith.build.quality

import java.nio.file.Files
import java.nio.file.Path

internal class RepositoryQualityValidator(private val policy: RepositoryQualityPolicy) {
    fun validate(repositoryRoot: Path, sourceFiles: Iterable<Path>): List<RepositoryQualityViolation> = sourceFiles
        .map { sourceFile -> sourceFile.toProductionKotlinSource(repositoryRoot) }
        .flatMap { source ->
            listOfNotNull(
                validateTopLevelProductionDeclarations(source),
                validateLineCount(source),
                validateMissingPackageDeclaration(source),
                validatePackageSegments(source),
                validatePackagePath(source),
                validatePrimaryTypeFileName(source),
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
            message = multipleProductionTypesMessage(declarationCount),
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
            message = productionFileLinesMessage(
                lineCount = source.lineCount,
                maxLines = maxLines,
                rationale = rationale,
            ),
        )
    }

    private fun validatePackageSegments(source: ProductionKotlinSource): RepositoryQualityViolation? {
        val packageName = source.packageName ?: return null
        val forbiddenSegment =
            packageName.split('.').firstOrNull(policy.forbiddenPackageSegments::contains) ?: return null

        return RepositoryQualityViolation(
            rule = "forbidden-package-segment",
            path = source.relativePath,
            message = forbiddenPackageSegmentMessage(
                packageName = packageName,
                forbiddenSegment = forbiddenSegment,
            ),
        )
    }

    private fun validateMissingPackageDeclaration(source: ProductionKotlinSource): RepositoryQualityViolation? {
        if (source.packageName != null) {
            return null
        }

        return RepositoryQualityViolation(
            rule = "missing-package-declaration",
            path = source.relativePath,
            message = missingPackageDeclarationMessage,
        )
    }

    private fun validatePackagePath(source: ProductionKotlinSource): RepositoryQualityViolation? {
        val packageName = source.packageName ?: return null
        val expectedDirectory = packageName.replace('.', '/')
        if (source.sourceRootRelativeDirectory == expectedDirectory) {
            return null
        }

        return RepositoryQualityViolation(
            rule = "package-path-mismatch",
            path = source.relativePath,
            message = packagePathMismatchMessage(
                packageName = packageName,
                expectedDirectory = expectedDirectory,
                actualDirectory = source.sourceRootRelativeDirectory,
            ),
        )
    }

    private fun validatePrimaryTypeFileName(source: ProductionKotlinSource): RepositoryQualityViolation? {
        val declarationName = source.topLevelProductionDeclarationNames.singleOrNull() ?: return null
        if (source.fileNameWithoutExtension == declarationName) {
            return null
        }

        return RepositoryQualityViolation(
            rule = "primary-type-file-name",
            path = source.relativePath,
            message = primaryTypeFileNameMessage(
                fileNameWithoutExtension = source.fileNameWithoutExtension,
                declarationName = declarationName,
            ),
        )
    }

    private fun Path.toProductionKotlinSource(repositoryRoot: Path): ProductionKotlinSource {
        val absolutePath = toAbsolutePath().normalize()
        val relativePath = repositoryRoot.relativize(absolutePath).toNormalizedPathString()
        val lines = Files.readAllLines(absolutePath)
        return ProductionKotlinSource(
            path = absolutePath,
            relativePath = relativePath,
            sourceRootRelativePath = relativePath.toSourceRootRelativePath(),
            lines = lines,
            packageName = lines.firstNotNullOfOrNull { line -> line.packageNameOrNull() },
            topLevelProductionDeclarationNames = lines.mapNotNull { line ->
                line.topLevelProductionDeclarationNameOrNull()
            },
        )
    }

    private fun Path.toNormalizedPathString(): String = toString().replace('\\', '/')

    private fun String.toSourceRootRelativePath(): String = when {
        startsWith(sourceRootRelativePrefix) -> removePrefix(sourceRootRelativePrefix)
        contains(sourceRootRelativeMarker) -> substringAfter(sourceRootRelativeMarker)
        else -> this
    }

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

    private fun String.topLevelProductionDeclarationNameOrNull(): String? {
        val line = trimEnd()
        if (line.isBlank() || line.first().isWhitespace() || line.startsWith("private ")) {
            return null
        }

        return topLevelTypeDeclarationNamePattern.find(line)?.groupValues?.get(1)
            ?: topLevelFunInterfaceNamePattern.find(line)?.groupValues?.get(1)
            ?: topLevelTypeAliasNamePattern.find(line)?.groupValues?.get(1)
    }

    private companion object {
        private const val sourceRootRelativePrefix = "src/main/kotlin/"
        private const val sourceRootRelativeMarker = "/src/main/kotlin/"
        private val packageDeclarationPattern = Regex("^package\\s+([A-Za-z0-9_.]+)$")
        private const val declarationModifierPattern =
            "(?:(?:public|internal|open|abstract|final|sealed|data|value|enum|annotation|expect|actual)\\s+)*"
        private val topLevelTypeDeclarationPattern = Regex(
            "^$declarationModifierPattern(?:class|interface|object)\\b",
        )
        private val topLevelFunInterfacePattern = Regex("^$declarationModifierPattern(?:fun\\s+interface)\\b")
        private val topLevelTypeAliasPattern = Regex("^$declarationModifierPattern(?:typealias)\\b")
        private val topLevelTypeDeclarationNamePattern = Regex(
            "^$declarationModifierPattern(?:class|interface|object)\\s+([A-Za-z_][A-Za-z0-9_]*)\\b",
        )
        private val topLevelFunInterfaceNamePattern = Regex(
            "^$declarationModifierPattern(?:fun\\s+interface)\\s+([A-Za-z_][A-Za-z0-9_]*)\\b",
        )
        private val topLevelTypeAliasNamePattern = Regex(
            "^$declarationModifierPattern(?:typealias)\\s+([A-Za-z_][A-Za-z0-9_]*)\\b",
        )

        private fun multipleProductionTypesMessage(declarationCount: Int): String =
            "$declarationCount non-private top-level production declarations found. " +
                "Split extra production types into their own files or make tightly coupled helpers private."

        private fun productionFileLinesMessage(lineCount: Int, maxLines: Int, rationale: String): String =
            "$lineCount lines exceeds the allowed maximum of $maxLines. " +
                "Split parsing, validation, rendering, diagnostics, policy, or I/O responsibilities " +
                "before extending the file further.$rationale"

        private fun forbiddenPackageSegmentMessage(packageName: String, forbiddenSegment: String): String =
            "Package '$packageName' contains forbidden segment '$forbiddenSegment'. " +
                "Use a domain-led package name instead of util/utils/misc."

        private const val missingPackageDeclarationMessage =
            "Production Kotlin files must declare an explicit package. " +
                "Use a domain-led package that matches the src/main/kotlin directory."

        private fun packagePathMismatchMessage(
            packageName: String,
            expectedDirectory: String,
            actualDirectory: String,
        ): String = "Package '$packageName' maps to '$expectedDirectory', but the source file lives under " +
            "'$actualDirectory'. Keep package declarations aligned with src/main/kotlin paths."

        private fun primaryTypeFileNameMessage(fileNameWithoutExtension: String, declarationName: String): String =
            "File '$fileNameWithoutExtension.kt' does not match the single top-level production declaration " +
                "'$declarationName'. Rename the file or the declaration so the owning type remains obvious."
    }
}
