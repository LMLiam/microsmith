package io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.reference

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReferencePathTests :
    StringSpec({
        "unqualified name appends to current segments" {
            getReferencePath(
                listOf("pkg", "sub"),
                "Foo",
            ) shouldBe
                listOf(
                    "pkg",
                    "sub",
                    "Foo",
                )
        }

        "qualified name with dot ignores current segments" {
            getReferencePath(
                listOf("pkg", "sub"),
                "apkg.Foo",
            ) shouldBe
                listOf(
                    "apkg",
                    "Foo",
                )
        }

        "relative with one dot goes up one segment" {
            getReferencePath(
                listOf("pkg", "sub"),
                ".Foo",
            ) shouldBe
                listOf(
                    "pkg",
                    "Foo",
                )
        }

        "relative with more dots than segments drops all" {
            getReferencePath(
                listOf("pkg", "sub"),
                "....Foo",
            ) shouldBe
                listOf("Foo")
        }

        "relative with nested path works correctly" {
            getReferencePath(
                listOf("a", "b", "c"),
                "..x.Y",
            ) shouldBe
                listOf(
                    "a",
                    "x",
                    "Y",
                )
        }

        "reference path rejects empty trailing segments" {
            shouldThrow<IllegalArgumentException> {
                getReferencePath(
                    listOf("pkg"),
                    ".",
                )
            }
        }
    })
