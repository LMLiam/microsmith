package io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved

import io.github.lmliam.microsmith.dsl.schemas.protobuf.numbers.ProtobufNumberLimits
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReservedTests :
    StringSpec({
        "fromRange returns ReservedIndex when first == last" {
            val reserved = Reserved.fromRange(5..5)
            reserved shouldBe ReservedIndex(5)
        }

        "fromRange returns ReservedToMax when last == ProtobufNumberLimits.FIELD_MAX" {
            val reserved = Reserved.fromRange(5..ProtobufNumberLimits.FIELD_MAX)
            reserved shouldBe ReservedToMax(5)
        }

        "fromRange returns ReservedRange for general ranges" {
            val reserved = Reserved.fromRange(10..20)
            reserved shouldBe ReservedRange(10..20)
        }
    })
