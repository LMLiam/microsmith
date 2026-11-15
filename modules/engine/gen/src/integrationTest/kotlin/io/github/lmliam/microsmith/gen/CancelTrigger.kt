package io.github.lmliam.microsmith.gen

import thirdparty.contract.ASSEMBLING_CODE
import thirdparty.contract.CONTRIBUTING_CODE
import thirdparty.contract.RESOLVING_CODE

enum class CancelTrigger(val diagnosticCode: String? = null) {
    TEMPORARY_DIRECTORY_REQUESTED,
    RESOLVING(RESOLVING_CODE),
    CONTRIBUTING(CONTRIBUTING_CODE),
    ASSEMBLING(ASSEMBLING_CODE),
}
