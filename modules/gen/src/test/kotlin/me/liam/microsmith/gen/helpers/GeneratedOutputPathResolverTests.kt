package me.liam.microsmith.gen.helpers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.paths.shouldBeAbsolute
import io.kotest.matchers.shouldBe
import me.liam.microsmith.gen.files.DirectorySpace
import java.nio.file.Files
import kotlin.io.path.Path

class GeneratedOutputPathResolverTests :
    StringSpec({
        "resolve resolves safe relative output paths under root" {
            val root = Files.createTempDirectory("microsmith-paths-root-")
            val space = DirectorySpace.from(root)

            val resolved = GeneratedOutputPathResolver.resolve(space, Path("proto/pkg/User.proto"))

            resolved.shouldBeAbsolute()
            resolved.startsWith(space.root) shouldBe true
            Files.exists(resolved) shouldBe false
        }

        "resolve rejects absolute generated output paths" {
            val root = Files.createTempDirectory("microsmith-paths-root-")
            val space = DirectorySpace.from(root)
            val absolutePath = root.resolve("outside.proto").toAbsolutePath()

            shouldThrow<IllegalArgumentException> {
                GeneratedOutputPathResolver.resolve(space, absolutePath)
            }
        }

        "resolve rejects root traversal output paths" {
            val root = Files.createTempDirectory("microsmith-paths-root-")
            val space = DirectorySpace.from(root)

            shouldThrow<IllegalArgumentException> {
                GeneratedOutputPathResolver.resolve(space, Path("../outside.proto"))
            }

            shouldThrow<IllegalArgumentException> {
                GeneratedOutputPathResolver.resolve(space, Path("safe/../../outside.proto"))
            }
        }

        "resolve rejects symlink traversal output paths" {
            val root = Files.createTempDirectory("microsmith-paths-root-")
            val outside = Files.createTempDirectory("microsmith-paths-outside-")
            val symlink = root.resolve("link")
            val linked = runCatching { Files.createSymbolicLink(symlink, outside) }.isSuccess
            if (linked) {
                val space = DirectorySpace.from(root)
                shouldThrow<IllegalArgumentException> {
                    GeneratedOutputPathResolver.resolve(space, Path("link/secret.proto"))
                }
            }
        }
    })
