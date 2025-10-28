package me.liam.microsmith.dsl.schemas.protobuf.support

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class NameRegistryTests : StringSpec({
    "new registry starts empty" {
        val registry = NameRegistry()
        registry.used() shouldBe emptySet()
        registry.reserved() shouldBe emptySet()
    }

    "use adds name to used set" {
        val registry = NameRegistry()
        registry.use("hello")
        registry.used() shouldContainExactly setOf("hello")
        registry.reserved() shouldBe emptySet()
    }

    "reserve adds name to reserved set" {
        val registry = NameRegistry()
        registry.reserve("hello")
        registry.used() shouldBe emptySet()
        registry.reserved() shouldContainExactly setOf("hello")
    }

    "cannot use a blank name" {
        val registry = NameRegistry()
        shouldThrow<IllegalArgumentException> {
            registry.use(" ")
        }
    }

    "cannot reserve a blank name" {
        val registry = NameRegistry()
        shouldThrow<IllegalArgumentException> {
            registry.reserve(" ")
        }
    }

    "cannot use a name already reserved" {
        val registry = NameRegistry()
        registry.reserve("hello")
        shouldThrow<IllegalArgumentException> {
            registry.use("hello")
        }
    }

    "cannot reserve a name already used" {
        val registry = NameRegistry()
        registry.use("hello")
        shouldThrow<IllegalArgumentException> {
            registry.reserve("hello")
        }
    }

    "cannot use a name already used" {
        val registry = NameRegistry()
        registry.use("hello")
        shouldThrow<IllegalArgumentException> {
            registry.use("hello")
        }
    }

    "cannot reserve a name already reserved" {
        val registry = NameRegistry()
        registry.reserve("hello")
        shouldThrow<IllegalArgumentException> {
            registry.reserve("hello")
        }
    }

    "used and reserved sets are immutable snapshots" {
        val registry = NameRegistry()
        registry.use("foo")
        registry.reserve("bar")

        val usedSnapshot = registry.used()
        val reservedSnapshot = registry.reserved()

        registry.use("baz")
        registry.reserve("qux")

        usedSnapshot shouldContainExactly setOf("foo")
        reservedSnapshot shouldContainExactly setOf("bar")

        registry.used() shouldContainExactly setOf("foo", "baz")
        registry.reserved() shouldContainExactly setOf("bar", "qux")
    }
})