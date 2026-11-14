package io.github.lmliam.microsmith.build.layers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private fun names(vararg internalNames: String) = internalNames.map(::JvmClassName)

class AbiDumpTests :
    StringSpec({

        "reads the classes in a method descriptor's parameters and return type" {
            val dump =
                listOf(
                    "public final class a/Host {",
                    "\tpublic final fun run (Lb/Input;ILc/Other;)Ld/Output;",
                    "}",
                )

            referencedClasses(dump).shouldContainExactly(names("b/Input", "c/Other", "d/Output"))
        }

        "reads the element class of an array descriptor" {
            val dump = listOf("public final class a/Host {", "\tpublic final fun all ()[Lb/Item;", "}")

            referencedClasses(dump).shouldContainExactly(names("b/Item"))
        }

        "reads the class of a field descriptor" {
            val dump = listOf("public final class a/Host {", "\tpublic static final field Companion La/Host\$Companion;", "}")

            referencedClasses(dump).shouldContainExactly(names("a/Host\$Companion"))
        }

        "reads every supertype from a class header" {
            val dump = listOf("public final class a/Host : b/Base, c/Marker, d/Other {", "}")

            referencedClasses(dump).shouldContainExactly(names("b/Base", "c/Marker", "d/Other"))
        }

        "reads the supertype of an interface header" {
            val dump = listOf("public abstract interface class a/Host : java/lang/AutoCloseable {", "}")

            referencedClasses(dump).shouldContainExactly(names("java/lang/AutoCloseable"))
        }

        "does not read the declaring class of a header without supertypes" {
            referencedClasses(listOf("public final class a/Host {", "}")).shouldBeEmpty()
        }

        "does not read a member name that looks like an object descriptor" {
            val dump = listOf("public final class a/Host {", "\tpublic final fun Lb/Name; ()V", "}")

            referencedClasses(dump).shouldBeEmpty()
        }

        "reads the descriptor of a member whose name contains spaces" {
            val dump = listOf("public final class a/Host {", "\tpublic final fun does the thing (Lb/Input;)V", "}")

            referencedClasses(dump).shouldContainExactly(names("b/Input"))
        }

        "lists each referenced class once, in order" {
            val dump =
                listOf(
                    "public final class a/Host : z/Base {",
                    "\tpublic final fun one (Lm/Shared;)Lm/Shared;",
                    "\tpublic final fun two (La/First;)V",
                    "}",
                )

            referencedClasses(dump).shouldContainExactly(names("a/First", "m/Shared", "z/Base"))
        }

        "treats a class as in a package when its package is that package or a subpackage" {
            val metro = JvmPackage("dev/zacsweers/metro")

            JvmClassName("dev/zacsweers/metro/Provider").isIn(metro) shouldBe true
            JvmClassName("dev/zacsweers/metro/internal/Hidden").isIn(metro) shouldBe true
        }

        "does not treat a class as in a package that merely shares its name prefix" {
            val metro = JvmPackage("dev/zacsweers/metro")

            JvmClassName("dev/zacsweers/metrology/Provider").isIn(metro) shouldBe false
            JvmClassName("dev/zacsweers/Metro").isIn(metro) shouldBe false
        }
    })
