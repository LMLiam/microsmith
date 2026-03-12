package me.liam.microsmith.gen.schemas.protobuf.emission

import me.liam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import me.liam.microsmith.gen.schemas.protobuf.names.ProtobufNameValidation

internal object ReservedEmissionValidator {
    fun validate(reserved: Reserved) {
        when (reserved) {
            is ReservedIndex -> ProtobufFieldNumbers.requireValidFieldNumber(reserved.index, "Reserved index")
            is ReservedRange -> validateRange(reserved)
            is ReservedToMax -> ProtobufFieldNumbers.requireValidFieldNumber(reserved.from, "Reserved-to-max start")
            is ReservedName -> ProtobufNameValidation.requireIdentifier(reserved.name, "Reserved name")
        }
    }

    private fun validateRange(range: ReservedRange) {
        require(range.indexRange.first <= range.indexRange.last) {
            "Reserved range must be ascending, but was ${range.indexRange}."
        }
        ProtobufFieldNumbers.requireValidFieldNumber(range.indexRange.first, "Reserved range start")
        ProtobufFieldNumbers.requireValidFieldNumber(range.indexRange.last, "Reserved range end")
    }
}
