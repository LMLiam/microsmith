package io.github.lmliam.microsmith.build.quality

import java.nio.file.Path

internal class RepositoryQualityValidator(private val policy: RepositoryQualityPolicy) {
    fun validate(repositoryRoot: Path, sourceFiles: Iterable<Path>): List<RepositoryQualityViolation> = sourceFiles
        .map { sourceFile -> ProductionKotlinSourceReader.read(repositoryRoot, sourceFile) }
        .flatMap(::validateSource)
        .sortedWith(compareBy(RepositoryQualityViolation::rule, RepositoryQualityViolation::path))

    private fun validateSource(source: ProductionKotlinSource): List<RepositoryQualityViolation> = listOfNotNull(
        validateTopLevelProductionDeclarations(source),
        validateLineCount(source),
        validateMissingPackageDeclaration(source),
        validatePackageSegments(source),
        validatePackagePath(source),
        validatePrimaryTypeFileName(source),
    )

    private fun validateTopLevelProductionDeclarations(source: ProductionKotlinSource): RepositoryQualityViolation? {
        if (policy.multiDeclarationExemptionFor(source.relativePath) != null) {
            return null
        }

        if (source.topLevelProductionDeclarationCount <= 1) {
            return null
        }

        return RepositoryQualityViolation(
            rule = "multiple-production-types",
            path = source.relativePath,
            message = multipleProductionTypesMessage(source.topLevelProductionDeclarationCount),
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
            message = MISSING_PACKAGE_DECLARATION_MESSAGE,
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

    private companion object {
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

        private const val MISSING_PACKAGE_DECLARATION_MESSAGE =
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
