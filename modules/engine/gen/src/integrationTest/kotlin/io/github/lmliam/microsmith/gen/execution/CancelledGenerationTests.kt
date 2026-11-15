package io.github.lmliam.microsmith.gen.execution

import io.github.lmliam.microsmith.gen.CancelTrigger
import io.github.lmliam.microsmith.gen.runCancelled
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.io.path.listDirectoryEntries
import thirdparty.contract.ASSEMBLING_CODE
import thirdparty.contract.COMPILING_CODE
import thirdparty.contract.CONTRIBUTING_CODE
import thirdparty.contract.RESOLVING_CODE

class CancelledGenerationTests :
    StringSpec(
        {
            "a run cancelled while contributing removes its temporary directory" {
                val run = runCancelled("alpha", "beta", trigger = CancelTrigger.CONTRIBUTING)

                run.temporaryRoot.listDirectoryEntries().shouldBeEmpty()
            }

            "a run cancelled while its temporary directory is being created removes it" {
                val run = runCancelled("alpha", "beta", trigger = CancelTrigger.TEMPORARY_DIRECTORY_REQUESTED)

                run.temporaryRoot.listDirectoryEntries().shouldBeEmpty()
            }

            val stagesReachedBeforeCancellation =
                mapOf(
                    CancelTrigger.TEMPORARY_DIRECTORY_REQUESTED to emptyList(),
                    CancelTrigger.RESOLVING to listOf(RESOLVING_CODE),
                    CancelTrigger.CONTRIBUTING to listOf(RESOLVING_CODE, CONTRIBUTING_CODE),
                    CancelTrigger.ASSEMBLING to listOf(RESOLVING_CODE, CONTRIBUTING_CODE, ASSEMBLING_CODE),
                )
            val stageCodes = listOf(RESOLVING_CODE, CONTRIBUTING_CODE, ASSEMBLING_CODE, COMPILING_CODE)

            for ((trigger, expectedStages) in stagesReachedBeforeCancellation) {
                "a run cancelled at $trigger does not start the next stage" {
                    val run = runCancelled("stages", trigger = trigger)

                    run.diagnostics.diagnostics.map { it.code }.filter { it in stageCodes } shouldBe expectedStages
                }
            }
        },
    )
