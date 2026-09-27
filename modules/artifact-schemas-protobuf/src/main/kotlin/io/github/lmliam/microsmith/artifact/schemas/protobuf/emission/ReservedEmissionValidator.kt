package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtobufNameValidation

internal fun validateReserved(reserved: Reserved) {
    when (reserved) {
        is ReservedIndex -> requireValidFieldNumber(reserved.index, "Reserved index")
        is ReservedRange -> validateReservedRange(reserved)
        is ReservedToMax -> requireValidFieldNumber(reserved.from, "Reserved-to-max start")
        is ReservedName -> ProtobufNameValidation.requireIdentifier(reserved.name, "Reserved name")
    }
}

private fun validateReservedRange(range: ReservedRange) {
    require(range.indexRange.first <= range.indexRange.last) {
        "Reserved range must be ascending, but was ${range.indexRange}"
    }
    requireValidFieldNumber(range.indexRange.first, "Reserved range start")
    requireValidFieldNumber(range.indexRange.last, "Reserved range end")
}
