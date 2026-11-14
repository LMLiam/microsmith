package io.github.lmliam.microsmith.artifact.files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RelativePathTests :
    StringSpec(
        {
            "segments are the non-empty parts of a slash-separated path" {
                RelativePath.of("schemas/orders.proto").segments shouldBe listOf("schemas", "orders.proto")
            }

            "separate arguments form the same path as a slash-separated string" {
                RelativePath.of("schemas", "orders.proto") shouldBe RelativePath.of("schemas/orders.proto")
            }

            "backslashes separate segments like slashes" {
                RelativePath.of("schemas\\orders.proto") shouldBe RelativePath.of("schemas/orders.proto")
            }

            "current-directory segments are dropped" {
                RelativePath.of("./schemas/./orders.proto").segments shouldBe listOf("schemas", "orders.proto")
            }

            "repeated and trailing separators are collapsed" {
                RelativePath.of("schemas//orders/").segments shouldBe listOf("schemas", "orders")
            }

            "no parts, an empty string and a current-directory segment all form the root" {
                listOf(RelativePath.of(), RelativePath.of(""), RelativePath.of(".")).forEach { path ->
                    path shouldBe RelativePath.ROOT
                    path.isRoot shouldBe true
                }
            }

            "a path with segments is not the root" { RelativePath.of("schemas").isRoot shouldBe false }

            "a leading slash is rejected as absolute" {
                shouldThrow<IllegalArgumentException> { RelativePath.of("/schemas/orders.proto") }
            }

            "a leading backslash is rejected as absolute" {
                shouldThrow<IllegalArgumentException> { RelativePath.of("\\schemas\\orders.proto") }
            }

            "a UNC path is rejected as absolute" {
                shouldThrow<IllegalArgumentException> { RelativePath.of("\\\\server\\share\\orders.proto") }
            }

            "a drive letter is rejected as absolute" {
                shouldThrow<IllegalArgumentException> { RelativePath.of("C:\\schemas\\orders.proto") }
            }

            "a drive letter inside the path is rejected as absolute" {
                shouldThrow<IllegalArgumentException> { RelativePath.of("schemas/C:orders.proto") }
            }

            "an absolute later part is rejected" {
                shouldThrow<IllegalArgumentException> { RelativePath.of("schemas", "/orders.proto") }
            }

            "a leading parent segment is rejected" {
                shouldThrow<IllegalArgumentException> { RelativePath.of("../outside.proto") }
            }

            "a parent segment inside the path is rejected even when it stays within the path" {
                shouldThrow<IllegalArgumentException> { RelativePath.of("schemas/../orders.proto") }
            }

            "a parent segment in a backslash-separated path is rejected" {
                shouldThrow<IllegalArgumentException> { RelativePath.of("schemas\\..\\orders.proto") }
            }

            "a name that only starts with dots is accepted" {
                RelativePath.of("..hidden", ".gitignore").segments shouldBe listOf("..hidden", ".gitignore")
            }

            "resolve appends child segments" {
                RelativePath.of("dotnet").resolve("Platform", "Orders.Api") shouldBe
                    RelativePath.of("dotnet/Platform/Orders.Api")
            }

            "resolving a child under the root yields the child" {
                RelativePath.ROOT.resolve("proto") shouldBe RelativePath.of("proto")
            }

            "resolve rejects a parent segment" {
                shouldThrow<IllegalArgumentException> { RelativePath.of("dotnet").resolve("..") }
            }

            "equal paths have equal hash codes" {
                RelativePath.of("a/b").hashCode() shouldBe RelativePath.of("a", "b").hashCode()
            }

            "different paths are not equal" { (RelativePath.of("a/b") == RelativePath.of("a/c")) shouldBe false }

            "toString joins segments with slashes" { RelativePath.of("a\\b", "c").toString() shouldBe "a/b/c" }

            "the root renders as a dot" { RelativePath.ROOT.toString() shouldBe "." }
        },
    )
