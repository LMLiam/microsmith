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
                        properties = mapOf(MsBuildNames.MANAGE_PACKAGE_VERSIONS_CENTRALLY_PROPERTY to "true"),
                        items =
                        listOf(
                            MsBuildItem(
                                itemName = MsBuildNames.PACKAGE_VERSION_ITEM,
                                include = "Serilog.AspNetCore",
                                attributes = mapOf(MsBuildNames.VERSION_ATTRIBUTE to "9.0.0"),
                            ),
                        ),
                    ),
                )

            val merged =
                assembler.merge(
                    initial,
                    MsBuildProjectContribution(
                        artifactId = artifactId,
                        properties = mapOf(MsBuildNames.MANAGE_PACKAGE_VERSIONS_CENTRALLY_PROPERTY to "true"),
                        items =
                        listOf(
                            MsBuildItem(
                                itemName = MsBuildNames.PACKAGE_VERSION_ITEM,
                                include = "FluentValidation.AspNetCore",
                                attributes = mapOf(MsBuildNames.VERSION_ATTRIBUTE to "12.0.0"),
                            ),
                        ),
                    ),
                )

            merged.properties shouldContainExactly
                mapOf(MsBuildNames.MANAGE_PACKAGE_VERSIONS_CENTRALLY_PROPERTY to "true")
            merged.items shouldContainExactly listOf(
                MsBuildItem(
                    itemName = MsBuildNames.PACKAGE_VERSION_ITEM,
                    include = "Serilog.AspNetCore",
                    attributes = mapOf(MsBuildNames.VERSION_ATTRIBUTE to "9.0.0"),
                ),
                MsBuildItem(
                    itemName = MsBuildNames.PACKAGE_VERSION_ITEM,
                    include = "FluentValidation.AspNetCore",
                    attributes = mapOf(MsBuildNames.VERSION_ATTRIBUTE to "12.0.0"),
                ),
            )
        }

        "merge rejects conflicting properties and item metadata" {
            val current =
                assembler.create(
                    MsBuildProjectContribution(
                        artifactId = artifactId,
                        properties = mapOf(MsBuildNames.MANAGE_PACKAGE_VERSIONS_CENTRALLY_PROPERTY to "true"),
                        items =
                        listOf(
                            MsBuildItem(
                                itemName = MsBuildNames.PACKAGE_VERSION_ITEM,
                                include = "Serilog.AspNetCore",
                                attributes = mapOf(MsBuildNames.VERSION_ATTRIBUTE to "9.0.0"),
                            ),
                        ),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                assembler.merge(
                    current,
                    MsBuildProjectContribution(
                        artifactId = artifactId,
                        properties = mapOf(MsBuildNames.MANAGE_PACKAGE_VERSIONS_CENTRALLY_PROPERTY to "false"),
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
                                itemName = MsBuildNames.PACKAGE_VERSION_ITEM,
                                include = "Serilog.AspNetCore",
                                attributes = mapOf(MsBuildNames.VERSION_ATTRIBUTE to "9.0.1"),
                            ),
                        ),
                    ),
                )
            }
        }

        "contributions and items reject invalid msbuild names" {
            shouldThrow<IllegalArgumentException> {
                MsBuildProjectContribution(
                    artifactId = artifactId,
                    properties = mapOf("Bad Property" to "true"),
                )
            }

            shouldThrow<IllegalArgumentException> {
                MsBuildItem(
                    itemName = "",
                    include = "Serilog.AspNetCore",
                )
            }

            shouldThrow<IllegalArgumentException> {
                MsBuildItem(
                    itemName = MsBuildNames.PACKAGE_VERSION_ITEM,
                    include = "Serilog.AspNetCore",
                    attributes = mapOf("Bad Attribute" to "9.0.0"),
                )
            }
        }

        "items snapshot attribute maps at construction time" {
            val sourceAttributes = linkedMapOf(MsBuildNames.VERSION_ATTRIBUTE to "9.0.0")

            val item =
                MsBuildItem(
                    itemName = MsBuildNames.PACKAGE_VERSION_ITEM,
                    include = "Serilog.AspNetCore",
                    attributes = sourceAttributes,
                )

            sourceAttributes["Bad Attribute"] = "oops"

            item.attributes shouldContainExactly mapOf(MsBuildNames.VERSION_ATTRIBUTE to "9.0.0")
        }

        "create preserves declared artifact identity" {
            val artifact = assembler.create(MsBuildProjectContribution(artifactId = artifactId))

            artifact.id shouldBe artifactId
        }
    })
