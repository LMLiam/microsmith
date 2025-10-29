package me.liam.microsmith.dsl.helpers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import me.liam.microsmith.dsl.core.MicrosmithExtension
import me.liam.microsmith.dsl.core.MicrosmithModel

data class FooExt(val foo: String) : MicrosmithExtension
data class BarExt(val bar: Int) : MicrosmithExtension

class MicrosmithModelExtensionsTests : StringSpec({
    "has() returns false when extension is missing" {
        val model = MicrosmithModel.empty()
        model.has<FooExt>().shouldBeFalse()
    }

    "has() returns true when extension is present" {
        val model = MicrosmithModel.empty().with(FooExt("hello"))
        model.has<FooExt>().shouldBeTrue()
    }

    "require() returns extension if present" {
        val model = MicrosmithModel.empty().with(FooExt("hello"))
        model.require<FooExt>().foo shouldBe "hello"
    }

    "require() throws if extension is missing" {
        val model = MicrosmithModel.empty()
        shouldThrow<IllegalStateException> {
            model.require<FooExt>()
        }
    }

    "extensions() returns all attached extensions" {
        val model = MicrosmithModel.empty().with(FooExt("hello")).with(BarExt(1))
        val exts = model.extensions()
        exts shouldContainExactlyInAnyOrder listOf(FooExt("hello"), BarExt(1))
    }

    "extensionTypes() returns the set of extension classes" {
        val model = MicrosmithModel.empty().with(FooExt("hello")).with(BarExt(1))
        model.extensionTypes() shouldContainExactlyInAnyOrder setOf(FooExt::class.java, BarExt::class.java)
    }
})