package io.github.lmliam.microsmith.build.quality

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
                "modules/cli/src/main/kotlin/io/github/lmliam/microsmith/cli/parsing/RunOptionsParser.kt" to
                    ProductionFileLineOverride(
                        maxLines = 220,
                        rationale =
                        "Run option parsing still owns several closely related option groups " +
                            "and will be decomposed separately rather than hidden behind " +
                            "a catch-all helper.",
                    ),
                "modules/dsl-schemas-protobuf/src/main/kotlin/io/github/lmliam/microsmith/dsl/schemas/protobuf/" +
                    "types/MessageBuilder.kt" to
                    ProductionFileLineOverride(
                        maxLines = 190,
                        rationale =
                        "Message builder orchestration is still being reduced in place; " +
                            "the temporary headroom is explicit until a narrower extraction " +
                            "is warranted.",
                    ),
                "modules/compile-services-dotnet/src/main/kotlin/io/github/lmliam/microsmith/compile/" +
                    "services/dotnet/csharp/CSharp.kt" to
                    ProductionFileLineOverride(
                        maxLines = 400,
                        rationale =
                        "The shared C# DSL intentionally keeps its core model and factory " +
                            "surface together; splitting it further made the generator API " +
                            "harder to read and use.",
                    ),
            ),
            multiDeclarationExemptions = emptyMap(),
            forbiddenPackageSegments = setOf("util", "utils", "misc"),
        )
    }
}
