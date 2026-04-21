package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.gen.files.GeneratedFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.charset.StandardCharsets
import kotlin.io.path.Path

class GeneratedOriginsManifestBuilderTests :
    StringSpec({
        "appendTo adds one origins manifest per output root" {
            val outputs = listOf(
                GeneratedFile(
                    relativePath = Path("proto/User.proto"),
                    contents = byteArrayOf(1),
                    outputRoot = Path("repo-a"),
                    origins = setOf("schemas.protobuf.pkg.User"),
                ),
                GeneratedFile(
                    relativePath = Path("Generated/Controllers/UserServiceApiControllerBase.cs"),
                    contents = byteArrayOf(2),
                    outputRoot = Path("repo-a"),
                    origins = setOf("services.UserService.rest.GetUser"),
                ),
                GeneratedFile(
                    relativePath = Path("Program.cs"),
                    contents = byteArrayOf(3),
                    outputRoot = Path("repo-b"),
                    origins = setOf("services.AdminService"),
                ),
            )

            val withManifest = GeneratedOriginsManifestBuilder.appendTo(outputs)
            val manifests = withManifest.filter { it.relativePath == Path(".microsmith/origins.json") }

            manifests.size shouldBe 2
            String(manifests.single { it.outputRoot == Path("repo-a") }.contents, StandardCharsets.UTF_8)
                .shouldContain("Generated/Controllers/UserServiceApiControllerBase.cs")
            String(manifests.single { it.outputRoot == Path("repo-a") }.contents, StandardCharsets.UTF_8)
                .shouldContain("schemas.protobuf.pkg.User")
            String(manifests.single { it.outputRoot == Path("repo-b") }.contents, StandardCharsets.UTF_8)
                .shouldContain("services.AdminService")
        }
    })
