package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.MessageRef
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReferenceTests :
    StringSpec(
        {
            val scope = listOf("pkg", "sub")

            "an unqualified name resolves inside the current scope" {
                Reference.parse("Foo").resolveSegments(scope) shouldBe listOf("pkg", "sub", "Foo")
            }

            "a qualified name ignores the current scope" {
                Reference.parse("apkg.Foo").resolveSegments(scope) shouldBe listOf("apkg", "Foo")
            }

            "a leading dot goes up one level" {
                Reference.parse(".Foo").resolveSegments(scope) shouldBe listOf("pkg", "Foo")
            }

            "more leading dots than scope segments drop the whole scope" {
                Reference.parse("....Foo").resolveSegments(scope) shouldBe listOf("Foo")
            }

            "a relative path keeps its nested segments" {
                Reference.parse("..x.Y").resolveSegments(listOf("a", "b", "c")) shouldBe listOf("a", "x", "Y")
            }

            "a symbolic reference ignores the current scope" {
                Reference.Symbolic(MessageRef("acme.shared.Foo")).resolveSegments(scope) shouldBe
                    listOf("acme", "shared", "Foo")
            }

            "a relative reference carries its levels and path" {
                Reference.parse("..x.Y") shouldBe Reference.Relative(levelsUp = 2, path = listOf("x", "Y"))
            }

            "a blank target is rejected" {
                shouldThrow<IllegalArgumentException> { Reference.parse(" ") }.message shouldBe
                    "Reference target cannot be blank."
            }

            "a target of only dots is rejected" {
                shouldThrow<IllegalArgumentException> { Reference.parse("..") }.message shouldBe
                    "Reference target cannot end with only dots: '..'"
            }

            "an empty path segment is rejected" {
                shouldThrow<IllegalArgumentException> { Reference.parse("a..B") }.message shouldBe
                    "Reference target contains empty path segments: 'a..B'"
            }

            "an empty path segment after leading dots is rejected" {
                shouldThrow<IllegalArgumentException> { Reference.parse(".a..B") }.message shouldBe
                    "Reference target contains empty path segments: '.a..B'"
            }

            "a trailing dot is rejected" {
                shouldThrow<IllegalArgumentException> { Reference.parse("a.B.") }.message shouldBe
                    "Reference target contains empty path segments: 'a.B.'"
            }

            "a relative reference that goes up no levels is rejected" {
                shouldThrow<IllegalArgumentException> { Reference.Relative(levelsUp = 0, path = listOf("Foo")) }
                    .message shouldBe "Relative reference must go up at least one level: 0"
            }

            "a relative reference with no path is rejected" {
                shouldThrow<IllegalArgumentException> { Reference.Relative(levelsUp = 1, path = emptyList()) }
                    .message shouldBe "Relative reference path contains empty segments: []"
            }

            "a relative reference with a blank path segment is rejected" {
                shouldThrow<IllegalArgumentException> { Reference.Relative(levelsUp = 1, path = listOf("a", "")) }
                    .message shouldBe "Relative reference path contains empty segments: [a, ]"
            }
        },
    )
