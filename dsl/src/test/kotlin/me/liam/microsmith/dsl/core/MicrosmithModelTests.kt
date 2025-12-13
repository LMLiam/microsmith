package me.liam.microsmith.dsl.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

data class FooExtension(
    val foo: String
) : MicrosmithExtension

data class BarExtension(
    val bar: Int
) : MicrosmithExtension

class MicrosmithModelTests :
    StringSpec({
        "empty model has no extensions" {
            val model = MicrosmithModel.empty()
            model.keys().isEmpty() shouldBe true
            model.get<FooExtension>().shouldBeNull()
        }

        "with attaches a new extension" {
            val model = MicrosmithModel.empty().with(FooExtension("hello"))

            model.get<FooExtension>()?.foo shouldBe "hello"
            model.get<BarExtension>().shouldBeNull()
        }

        "with replaces an existing extension of the same type" {
            val model1 = MicrosmithModel.empty().with(FooExtension("first"))
            val model2 = model1.with(FooExtension("second"))

            model2 shouldNotBe model1
            model1.get<FooExtension>()?.foo shouldBe "first"
            model2.get<FooExtension>()?.foo shouldBe "second"
        }

        "with can attach multiple different extensions" {
            val model = MicrosmithModel.empty().with(FooExtension("first")).with(BarExtension(1))

            model.get<FooExtension>()?.foo shouldBe "first"
            model.get<BarExtension>()?.bar shouldBe 1

            model.keys().map { it.simpleName }.toSet() shouldContainExactly
                setOf(
                    FooExtension::class.simpleName,
                    BarExtension::class.simpleName
                )
        }

        "get by KClass works the same as reified get" {
            val model = MicrosmithModel.empty().with(FooExtension("first"))

            model.get(FooExtension::class)?.foo shouldBe "first"
            model.get<FooExtension>()?.foo shouldBe "first"
        }
    })