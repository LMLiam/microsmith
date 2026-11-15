package io.github.lmliam.microsmith.gen.execution

import io.github.lmliam.microsmith.gen.CancelTrigger
import io.github.lmliam.microsmith.gen.runCancelled
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import kotlin.io.path.listDirectoryEntries

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
        },
    )
