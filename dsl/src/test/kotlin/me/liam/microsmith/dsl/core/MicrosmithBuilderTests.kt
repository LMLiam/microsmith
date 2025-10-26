package me.liam.microsmith.dsl.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

data class DummyExtension(val value: String) : MicrosmithExtension

class MicrosmithBuilderTests : StringSpec({
    "builder starts with an empty model" {
        val builder = MicrosmithBuilder()
        builder.model.keys().isEmpty() shouldBe true
    }

    "put attaches a new extension" {
        val builder = MicrosmithBuilder()
        builder.put(DummyExtension("hello"))

        val ext = builder.model.get<DummyExtension>()
        ext shouldNotBe null
        ext?.value shouldBe "hello"
    }

    "put replaces an existing extension of the same type" {
        val builder = MicrosmithBuilder()
        builder.put(DummyExtension("first"))
        val model1 = builder.model

        builder.put(DummyExtension("second"))
        val model2 = builder.model

        model2 shouldNotBe model1
        model2.get<DummyExtension>()?.value shouldBe "second"
    }

    "put does not affect previously returned model snapshots" {
        val builder = MicrosmithBuilder()
        builder.put(DummyExtension("initial"))
        val snapshot = builder.model

        builder.put(DummyExtension("updated"))
        val updatedSnapshot = builder.model

        snapshot.get<DummyExtension>()?.value shouldBe "initial"
        updatedSnapshot.get<DummyExtension>()?.value shouldBe "updated"
    }
})