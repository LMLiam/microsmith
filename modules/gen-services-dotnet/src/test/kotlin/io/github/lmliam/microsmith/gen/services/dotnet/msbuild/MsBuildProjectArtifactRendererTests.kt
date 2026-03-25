package io.github.lmliam.microsmith.gen.services.dotnet.msbuild

import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildItem
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifactId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path

class MsBuildProjectArtifactRendererTests :
    StringSpec({
        "render writes deterministic xml with escaped values" {
            val artifact =
                MsBuildProjectArtifact(
                    id = MsBuildProjectArtifactId(
                        relativePath = Path.of("Directory.Packages.props"),
                        outputRoot = Path.of("dotnet", "Platform"),
                    ),
                    properties = mapOf("ManagePackageVersionsCentrally" to "true"),
                    items =
                    listOf(
                        MsBuildItem(
                            type = "PackageVersion",
                            include = "Serilog.AspNetCore",
                            metadata = mapOf("Version" to "9.0.0 & preview"),
                        ),
                    ),
                )

            val generated = MsBuildProjectArtifactRenderer().render(artifact)
            val contents = generated.contents.toString(Charsets.UTF_8)

            generated.relativePath shouldBe Path.of("Directory.Packages.props")
            generated.outputRoot shouldBe Path.of("dotnet", "Platform")
            contents.shouldContain("<ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>")
            contents.shouldContain("<PackageVersion Include=\"Serilog.AspNetCore\" Version=\"9.0.0 &amp; preview\" />")
        }
    })
