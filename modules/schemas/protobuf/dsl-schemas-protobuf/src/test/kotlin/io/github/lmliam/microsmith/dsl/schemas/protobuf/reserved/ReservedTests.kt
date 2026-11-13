package io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReservedTests :
    StringSpec({
        "fromRange returns ReservedIndex when first == last" {
            Reserved.fromRange(5..5, max = 100) shouldBe ReservedIndex(5)
        }

        "fromRange returns ReservedToMax when last equals the maximum" {
            Reserved.fromRange(5..100, max = 100) shouldBe ReservedToMax(5)
        }

        "fromRange returns ReservedRange for a range below the maximum" {
            Reserved.fromRange(10..20, max = 100) shouldBe ReservedRange(10..20)
        }
    })
