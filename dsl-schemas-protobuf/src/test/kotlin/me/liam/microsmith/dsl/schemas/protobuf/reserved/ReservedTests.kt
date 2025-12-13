package me.liam.microsmith.dsl.schemas.protobuf.reserved

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReservedTests :
    StringSpec({
        "fromRange returns ReservedIndex when first == last" {
            val reserved = Reserved.fromRange(5..5)
            reserved shouldBe ReservedIndex(5)
        }

        "fromRange returns ReservedToMax when last == Max.VALUE" {
            val reserved = Reserved.fromRange(5..Max.VALUE)
            reserved shouldBe ReservedToMax(5)
        }

        "fromRange returns ReservedRange for general ranges" {
            val reserved = Reserved.fromRange(10..20)
            reserved shouldBe ReservedRange(10..20)
        }
    })