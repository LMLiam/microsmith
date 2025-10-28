package me.liam.microsmith.dsl.schemas.protobuf.field

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class OneofFieldBuilderTests : StringSpec({
    "default index is null" {
        val builder = OneofFieldBuilder()
        builder.index shouldBe null
    }

    "sets index correctly" {
        val builder = OneofFieldBuilder()
        builder.index(7)
        builder.index shouldBe 7
    }

    "reassigning index overwrites previous value" {
        val builder = OneofFieldBuilder()
        builder.index(7)
        builder.index(13)
        builder.index shouldBe 13
    }
})