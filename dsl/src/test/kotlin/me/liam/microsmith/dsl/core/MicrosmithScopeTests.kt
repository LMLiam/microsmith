package me.liam.microsmith.dsl.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import me.liam.microsmith.dsl.helpers.put

data class TestExtension(
    val value: String
) : MicrosmithExtension

class MicrosmithScopeTests :
    StringSpec({
        "microsmith returns an empty model when no extensions are added" {
            val model = microsmith { }
            model.keys().isEmpty() shouldBe true
        }

        "microsmith block can attach extensions via builder" {
            val model =
                microsmith {
                    (this as MicrosmithBuilder).put(TestExtension("hello"))
                }

            model.get<TestExtension>()?.value shouldBe "hello"
        }

        "each microsmith invocation returns a fresh immutable model" {
            val model1 =
                microsmith {
                    (this as MicrosmithBuilder).put(TestExtension("first"))
                }
            val model2 =
                microsmith {
                    (this as MicrosmithBuilder).put(TestExtension("second"))
                }
            model1.get<TestExtension>()?.value shouldBe "first"
            model2.get<TestExtension>()?.value shouldBe "second"
        }
    })