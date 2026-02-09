package me.liam.microsmith.gen.helpers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.paths.shouldBeAbsolute
import me.liam.microsmith.dsl.core.microsmith
import me.liam.microsmith.gen.files.DirectorySpace
import java.nio.file.Files
import kotlin.io.path.Path

class MicrosmithModelExtensionsTests :
    StringSpec({
        "resolveTargetPath resolves safe relative output paths under root" {
            val root = Files.createTempDirectory("microsmith-paths-root-")
            val space = DirectorySpace.from(root)

            val resolved = resolveTargetPath(space, Path("proto/pkg/User.proto"))

            resolved.shouldBeAbsolute()
            resolved.startsWith(space.root) shouldBe true
            Files.exists(resolved) shouldBe false
        }

        "resolveTargetPath rejects absolute generated output paths" {
            val root = Files.createTempDirectory("microsmith-paths-root-")
            val space = DirectorySpace.from(root)
            val absolutePath = root.resolve("outside.proto").toAbsolutePath()

            shouldThrow<IllegalArgumentException> {
                resolveTargetPath(space, absolutePath)
            }
        }

        "resolveTargetPath rejects root traversal output paths" {
            val root = Files.createTempDirectory("microsmith-paths-root-")
            val space = DirectorySpace.from(root)

            shouldThrow<IllegalArgumentException> {
                resolveTargetPath(space, Path("../outside.proto"))
            }

            shouldThrow<IllegalArgumentException> {
                resolveTargetPath(space, Path("safe/../../outside.proto"))
            }
        }

        "resolveTargetPath rejects symlink traversal output paths" {
            val root = Files.createTempDirectory("microsmith-paths-root-")
            val outside = Files.createTempDirectory("microsmith-paths-outside-")
            val symlink = root.resolve("link")
            val linked = runCatching { Files.createSymbolicLink(symlink, outside) }.isSuccess
            if (linked) {
                val space = DirectorySpace.from(root)
                shouldThrow<IllegalArgumentException> {
                    resolveTargetPath(space, Path("link/secret.proto"))
                }
            }
        }

        "generateTo creates output directory when it does not exist" {
            val workspaceRoot = Files.createTempDirectory("microsmith-generate-to-root-")
            val outputDir = workspaceRoot.resolve("generated/proto")
            val model = microsmith { }

            model.generateTo(outputDir)

            Files.exists(outputDir) shouldBe true
        }
    })
