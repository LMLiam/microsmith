package me.liam.microsmith.build.quality

internal data class RepositoryQualityPolicy(
    val defaultMaxProductionFileLines: Int,
    val productionFileLineOverrides: Map<String, ProductionFileLineOverride>,
    val multiDeclarationExemptions: Map<String, String>,
    val forbiddenPackageSegments: Set<String>,
) {
    fun maxLinesFor(relativePath: String): Int =
        productionFileLineOverrides[relativePath]?.maxLines ?: defaultMaxProductionFileLines

    fun multiDeclarationExemptionFor(relativePath: String): String? = multiDeclarationExemptions[relativePath]

    companion object {
        fun default(): RepositoryQualityPolicy = RepositoryQualityPolicy(
            defaultMaxProductionFileLines = 170,
            productionFileLineOverrides = mapOf(
                "cli/src/main/kotlin/me/liam/microsmith/cli/parsing/RunOptionsParser.kt" to ProductionFileLineOverride(
                    maxLines = 220,
                    rationale = @Suppress("ktlint:standard:max-line-length")
                    "Run option parsing still owns several closely related option groups and will be decomposed separately rather than hidden behind a catch-all helper.",
                ),
                "dsl-schemas-protobuf/src/main/kotlin/me/liam/microsmith/dsl/schemas/protobuf/types/MessageBuilder.kt" to
                    ProductionFileLineOverride(
                        maxLines = 190,
                        rationale = @Suppress("ktlint:standard:max-line-length")
                        "Message builder orchestration is still being reduced in place; the temporary headroom is explicit until a narrower extraction is warranted.",
                    ),
            ),
            multiDeclarationExemptions = emptyMap(),
            forbiddenPackageSegments = setOf("util", "utils", "misc"),
        )
    }
}
