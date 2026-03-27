package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe

class MsBuildProjectArtifactAssemblerTests :
    StringSpec({
        val assembler = MsBuildProjectArtifactAssembler()
        val artifactId = MsBuildProjectArtifactId(
            solutionName = "Platform",
            kind = MsBuildProjectKind.DirectoryPackagesProps,
        )

        "merge combines compatible properties and items" {
            val initial =
                assembler.create(
                    MsBuildProjectContribution(
                        artifactId = artifactId,
                        properties = mapOf("ManagePackageVersionsCentrally" to "true"),
                        items =
                        listOf(
                            MsBuildItem(
                                itemName = "PackageVersion",
                                include = "Serilog.AspNetCore",
                                attributes = mapOf(MsBuildAttributeName.Version to "9.0.0"),
                            ),
                        ),
                    ),
                )

            val merged =
                assembler.merge(
                    initial,
                    MsBuildProjectContribution(
                        artifactId = artifactId,
                        properties = mapOf("ManagePackageVersionsCentrally" to "true"),
                        items =
                        listOf(
                            MsBuildItem(
                                itemName = "PackageVersion",
                                include = "FluentValidation.AspNetCore",
                                attributes = mapOf(MsBuildAttributeName.Version to "12.0.0"),
                            ),
                        ),
                    ),
                )

            merged.properties shouldContainExactly mapOf("ManagePackageVersionsCentrally" to "true")
            merged.items shouldContainExactly listOf(
                MsBuildItem(
                    itemName = "PackageVersion",
                    include = "Serilog.AspNetCore",
                    attributes = mapOf(MsBuildAttributeName.Version to "9.0.0"),
                ),
                MsBuildItem(
                    itemName = "PackageVersion",
                    include = "FluentValidation.AspNetCore",
                    attributes = mapOf(MsBuildAttributeName.Version to "12.0.0"),
                ),
            )
        }

        "merge rejects conflicting properties and item metadata" {
            val current =
                assembler.create(
                    MsBuildProjectContribution(
                        artifactId = artifactId,
                        properties = mapOf("ManagePackageVersionsCentrally" to "true"),
                        items =
                        listOf(
                            MsBuildItem(
                                itemName = "PackageVersion",
                                include = "Serilog.AspNetCore",
                                attributes = mapOf(MsBuildAttributeName.Version to "9.0.0"),
                            ),
                        ),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                assembler.merge(
                    current,
                    MsBuildProjectContribution(
                        artifactId = artifactId,
                        properties = mapOf("ManagePackageVersionsCentrally" to "false"),
                    ),
                )
            }

            shouldThrow<IllegalArgumentException> {
                assembler.merge(
                    current,
                    MsBuildProjectContribution(
                        artifactId = artifactId,
                        items =
                        listOf(
                            MsBuildItem(
                                itemName = "PackageVersion",
                                include = "Serilog.AspNetCore",
                                attributes = mapOf(MsBuildAttributeName.Version to "9.0.1"),
                            ),
                        ),
                    ),
                )
            }
        }

        "create preserves declared artifact identity" {
            val artifact = assembler.create(MsBuildProjectContribution(artifactId = artifactId))

            artifact.id shouldBe artifactId
        }
    })
