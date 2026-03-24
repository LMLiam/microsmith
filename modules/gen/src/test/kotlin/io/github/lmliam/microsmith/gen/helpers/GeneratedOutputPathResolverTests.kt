package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.paths.shouldBeAbsolute
import io.kotest.matchers.shouldBe
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

        "resolve routes outputs into a nested output root" {
            val root = Files.createTempDirectory("microsmith-paths-root-")
            val space = DirectorySpace.from(root)
            val output =
                GeneratedFile(
                    relativePath = Path("UserService.cs"),
                    contents = byteArrayOf(),
                    outputRoot = Path("services/UserService"),
                )

            val resolved = GeneratedOutputPathResolver.resolve(space, output)

            resolved.shouldBeAbsolute()
            resolved.startsWith(space.root.resolve("services/UserService")) shouldBe true
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

        "resolve rejects absolute routed output roots" {
            val root = Files.createTempDirectory("microsmith-paths-root-")
            val space = DirectorySpace.from(root)
            val output =
                GeneratedFile(
                    relativePath = Path("UserService.cs"),
                    contents = byteArrayOf(),
                    outputRoot = root.resolve("services").toAbsolutePath(),
                )

            shouldThrow<IllegalArgumentException> {
                GeneratedOutputPathResolver.resolve(space, output)
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

        "resolve rejects routed output roots that traverse outside the root" {
            val root = Files.createTempDirectory("microsmith-paths-root-")
            val space = DirectorySpace.from(root)
            val output =
                GeneratedFile(
                    relativePath = Path("UserService.cs"),
                    contents = byteArrayOf(),
                    outputRoot = Path("../services/UserService"),
                )

            shouldThrow<IllegalArgumentException> {
                GeneratedOutputPathResolver.resolve(space, output)
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
