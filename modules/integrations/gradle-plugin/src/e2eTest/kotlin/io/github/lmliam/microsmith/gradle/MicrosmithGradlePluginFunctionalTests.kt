package io.github.lmliam.microsmith.gradle

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

class MicrosmithGradlePluginFunctionalTests : StringSpec() {
    init {
        "plugin registers task and IDE configurations for Java projects" {
            val project =
                MicrosmithGradleFunctionalTestProject.create(
                    name = "microsmith-gradle-plugin-java-wiring",
                    buildScriptResource = "/gradle/plugin/java-wiring.build.gradle.kts",
                )

            val result = project.build("verifyMicrosmithWiring")

            result.task(":verifyMicrosmithWiring")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        "microsmithGenerate uses the default script and output layout" {
            val project =
                MicrosmithGradleFunctionalTestProject.create(
                    name = "microsmith-gradle-plugin-default-generate",
                    buildScriptResource = "/gradle/plugin/microsmith-plugin.build.gradle.kts",
                )
            project.writeResource("build.microsmith.kts", "/gradle/plugin/default-protobuf.microsmith.kts")

            val result = project.build("microsmithGenerate")

            result.task(":microsmithGenerate")?.outcome shouldBe TaskOutcome.SUCCESS
            project.file("proto/GradleDefaultUserCreated.proto").toFile().shouldExist()
            result.output shouldContain "Generated Microsmith outputs into"
        }

        "microsmithGenerate honors custom script, output, vars, and flags" {
            val project =
                MicrosmithGradleFunctionalTestProject.create(
                    name = "microsmith-gradle-plugin-custom-generate",
                    buildScriptResource = "/gradle/plugin/custom-script-and-output.build.gradle.kts",
                )
            project.writeResource("schema.microsmith.kts", "/gradle/plugin/custom-vars-and-flags.microsmith.kts")

            val result = project.build("microsmithGenerate")

            result.task(":microsmithGenerate")?.outcome shouldBe TaskOutcome.SUCCESS
            project.file("custom-generated/proto/GradleConfiguredUserCreated.proto").toFile().shouldExist()
        }

        "microsmithGenerate supports ASP.NET service generation" {
            val project =
                MicrosmithGradleFunctionalTestProject.create(
                    name = "microsmith-gradle-plugin-dotnet-asp",
                    buildScriptResource = "/gradle/plugin/microsmith-plugin.build.gradle.kts",
                )
            project.writeResource("build.microsmith.kts", "/gradle/plugin/asp-user-service.microsmith.kts")

            val result = project.build("microsmithGenerate")

            result.task(":microsmithGenerate")?.outcome shouldBe TaskOutcome.SUCCESS
            project.file("dotnet/Platform/UserService.Api/Program.cs").toFile().shouldExist()
            project
                .file("dotnet/Platform/UserService.Api/Generated/Controllers/" + "UserServiceApiControllerBase.cs")
                .toFile()
                .shouldExist()
        }

        "microsmithGenerate fails with script diagnostics" {
            val project =
                MicrosmithGradleFunctionalTestProject.create(
                    name = "microsmith-gradle-plugin-failure",
                    buildScriptResource = "/gradle/plugin/microsmith-plugin.build.gradle.kts",
                )
            project.writeResource("build.microsmith.kts", "/gradle/plugin/unknown-call.microsmith.kts")

            val result = project.buildAndFail("microsmithGenerate")

            result.output shouldContain "Microsmith generation failed"
            result.output shouldContain "Unresolved reference 'unknownCall'"
        }
    }
}
