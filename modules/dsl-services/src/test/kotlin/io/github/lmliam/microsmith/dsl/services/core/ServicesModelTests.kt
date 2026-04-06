package io.github.lmliam.microsmith.dsl.services.core

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.helpers.require
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

private data class MergeableTestSharedExtension(val values: List<String>) :
    MicrosmithExtension,
    MergeableExtension<MergeableTestSharedExtension> {
    override fun merge(other: MergeableTestSharedExtension) = MergeableTestSharedExtension(values + other.values)
}

class ServicesModelTests :
    StringSpec({
        "merge combines mergeable services-level extensions by type" {
            val left =
                ServicesModel.empty().with(
                    MergeableTestSharedExtension::class,
                    MergeableTestSharedExtension(listOf("left")),
                )
            val right =
                ServicesModel.empty().with(
                    MergeableTestSharedExtension::class,
                    MergeableTestSharedExtension(listOf("right")),
                )

            left
                .merge(right)
                .require<MergeableTestSharedExtension>()
                .values shouldContainExactly listOf("left", "right")
        }
    })
