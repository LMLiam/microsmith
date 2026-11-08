package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ProtoIdentifierTests :
    StringSpec({
        "parse keeps identifiers that start with a letter or underscore" {
            ProtoIdentifier.parse("_status_2").getOrNull()?.value shouldBe "_status_2"
        }

        "parse rejects identifiers that start with a digit" { ProtoIdentifier.parse("2status").isLeft() shouldBe true }

        "parse rejects blank text" {
            ProtoIdentifier.parse("").isLeft() shouldBe true
            ProtoIdentifier.parse("   ").isLeft() shouldBe true
        }

        "parse rejects identifiers padded with whitespace" {
            ProtoIdentifier.parse(" status").isLeft() shouldBe true
            ProtoIdentifier.parse("status ").isLeft() shouldBe true
        }

        "parse rejects characters outside the identifier alphabet" {
            ProtoIdentifier.parse("user-name").isLeft() shouldBe true
        }
    })
