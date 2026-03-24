package io.github.lmliam.microsmith.dsl.services.core

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private data class TestServiceExtension(val value: String) : ServiceExtension
private data class MergeableTestServiceExtension(
    val values: List<String>,
) : ServiceExtension, MergeableExtension<MergeableTestServiceExtension> {
    override fun merge(other: MergeableTestServiceExtension) = MergeableTestServiceExtension(values + other.values)
}

class ServiceModelTests :
    StringSpec({
        "empty models compare equal by value" {
            ServiceModel.empty() shouldBe ServiceModel.empty()
        }

        "get returns extension when present" {
            val extension = TestServiceExtension("hello")
            val model = ServiceModel.empty().with(TestServiceExtension::class, extension)

            model.get<TestServiceExtension>() shouldBe extension
        }

        "get returns null when extension not present" {
            val model = ServiceModel.empty()

            model.get<TestServiceExtension>() shouldBe null
        }

        "with returns new immutable snapshot" {
            val model1 = ServiceModel.empty()
            val model2 = model1.with(TestServiceExtension::class, TestServiceExtension("hello"))

            model1.get<TestServiceExtension>() shouldBe null
            model2.get<TestServiceExtension>() shouldBe TestServiceExtension("hello")
        }

        "merge combines mergeable service extensions by type" {
            val left =
                ServiceModel.empty().with(
                    MergeableTestServiceExtension::class,
                    MergeableTestServiceExtension(listOf("left")),
                )
            val right =
                ServiceModel.empty().with(
                    MergeableTestServiceExtension::class,
                    MergeableTestServiceExtension(listOf("right")),
                )

            left.merge(right).get<MergeableTestServiceExtension>()!!.values shouldContainExactly listOf("left", "right")
        }
    })
