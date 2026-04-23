package io.github.lmliam.microsmith.runtime.scripting.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class GeneratedOutputRootsLocatorTests :
    StringSpec({
        "locate discovers generated output roots from origins manifests" {
            val outputDir = Files.createTempDirectory("generated-output-roots-")
            val protoManifest = outputDir.resolve("proto/.microsmith/origins.json")
            val aspManifest = outputDir.resolve("dotnet/Platform/UserService.Api/.microsmith/origins.json")
            protoManifest.parent.createDirectories()
            aspManifest.parent.createDirectories()
            protoManifest.writeText("""{"files":[]}""")
            aspManifest.writeText("""{"files":[]}""")

            GeneratedOutputRootsLocator.locate(outputDir) shouldContainExactly listOf(
                outputDir.resolve("dotnet/Platform/UserService.Api").toAbsolutePath().normalize(),
                outputDir.resolve("proto").toAbsolutePath().normalize(),
            )
        }

        "describe summarizes multiple generated roots" {
            val outputDir = Files.createTempDirectory("generated-output-root-description-")
            val protoManifest = outputDir.resolve("proto/.microsmith/origins.json")
            val aspManifest = outputDir.resolve("dotnet/Platform/UserService.Api/.microsmith/origins.json")
            protoManifest.parent.createDirectories()
            aspManifest.parent.createDirectories()
            protoManifest.writeText("""{"files":[]}""")
            aspManifest.writeText("""{"files":[]}""")

            val description = GeneratedOutputRootsLocator.describe(outputDir)

            description.shouldContain(outputDir.toAbsolutePath().normalize().toString())
            description.shouldContain(outputDir.resolve("proto").toAbsolutePath().normalize().toString())
            description.shouldContain(
                outputDir.resolve("dotnet/Platform/UserService.Api").toAbsolutePath().normalize().toString(),
            )
        }
    })
