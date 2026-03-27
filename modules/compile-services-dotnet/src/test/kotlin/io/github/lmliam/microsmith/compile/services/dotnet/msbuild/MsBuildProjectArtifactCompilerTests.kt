package io.github.lmliam.microsmith.compile.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildAttributeName
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItemName
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildPropertyName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

class MsBuildProjectArtifactCompilerTests :
    StringSpec({
        "compile emits deterministic xml with escaped values" {
            val artifact =
                MsBuildProjectArtifact(
                    id = MsBuildProjectArtifactId(
                        solutionName = "Platform",
                        kind = MsBuildProjectKind.DirectoryPackagesProps,
                    ),
                    properties = mapOf(MsBuildPropertyName.ManagePackageVersionsCentrally to "true"),
                    items =
                    listOf(
                        MsBuildItem(
                            itemName = MsBuildItemName.PackageVersion,
                            include = "Serilog.AspNetCore",
                            attributes = mapOf(MsBuildAttributeName.Version to "9.0.0 & preview"),
                        ),
                    ),
                )

            val contribution = MsBuildProjectArtifactCompiler().compile(artifact).single()
            val textContribution = contribution as TextFileArtifactContribution
            val contents = textContribution.contents

            textContribution.artifactId.relativePath shouldBe java.nio.file.Path.of("Directory.Packages.props")
            textContribution.artifactId.outputRoot shouldBe java.nio.file.Path.of("dotnet", "Platform")
            contents.shouldContain("<ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>")
            contents.shouldContain("<PackageVersion Include=\"Serilog.AspNetCore\" Version=\"9.0.0 &amp; preview\"/>")
        }

        "compile writes project package references into auto-imported Directory.Build.props" {
            val artifact =
                MsBuildProjectArtifact(
                    id = MsBuildProjectArtifactId(
                        solutionName = "Platform",
                        projectName = "UserService.Api",
                        kind = MsBuildProjectKind.DirectoryBuildProps,
                    ),
                    properties = emptyMap(),
                    items =
                    listOf(
                        MsBuildItem(
                            itemName = MsBuildItemName.PackageReference,
                            include = "Serilog.AspNetCore",
                            attributes = emptyMap(),
                        ),
                    ),
                )

            val contribution = MsBuildProjectArtifactCompiler().compile(artifact).single()
            val textContribution = contribution as TextFileArtifactContribution

            textContribution.artifactId.relativePath shouldBe java.nio.file.Path.of("Directory.Build.props")
            textContribution.artifactId.outputRoot shouldBe
                java.nio.file.Path.of("dotnet", "Platform", "UserService.Api")
            textContribution.contents.shouldContain("<PackageReference Include=\"Serilog.AspNetCore\"/>")
        }

        "compile emits well-formed xml for escaped property and attribute values" {
            val artifact =
                MsBuildProjectArtifact(
                    id = MsBuildProjectArtifactId(
                        solutionName = "Platform",
                        kind = MsBuildProjectKind.DirectoryPackagesProps,
                    ),
                    properties = mapOf(MsBuildPropertyName.ManagePackageVersionsCentrally to "A&B<true>"),
                    items =
                    listOf(
                        MsBuildItem(
                            itemName = MsBuildItemName.PackageVersion,
                            include = "Serilog.\"AspNetCore\"",
                            attributes = mapOf(MsBuildAttributeName.Version to "9.0.0 & preview"),
                        ),
                    ),
                )

            val contribution = MsBuildProjectArtifactCompiler().compile(artifact).single()
            val textContribution = contribution as TextFileArtifactContribution
            val contents = textContribution.contents

            contents.shouldContain("<ManagePackageVersionsCentrally>A&amp;B&lt;true&gt;</ManagePackageVersionsCentrally>")
            contents.shouldContain("""<PackageVersion Include="Serilog.&quot;AspNetCore&quot;" Version="9.0.0 &amp; preview"/>""")
            parseXml(contents) shouldBe "Project"
        }
    })

private fun parseXml(contents: String): String {
    val reader = XMLInputFactory.newFactory().createXMLStreamReader(StringReader(contents))
    var rootName: String? = null

    try {
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                rootName = rootName ?: reader.localName
            }
        }
    } finally {
        reader.close()
    }

    return requireNotNull(rootName)
}
