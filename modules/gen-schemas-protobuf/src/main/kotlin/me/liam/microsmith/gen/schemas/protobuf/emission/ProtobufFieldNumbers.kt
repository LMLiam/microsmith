package me.liam.microsmith.gen.schemas.protobuf.emission

internal object ProtobufFieldNumbers {
    internal const val MIN_FIELD_NUMBER = 1
    internal const val MAX_FIELD_NUMBER = 536_870_911
    internal val FORBIDDEN_RANGE: IntRange = 19_000..19_999

    internal fun requireValidFieldNumber(number: Int, label: String) {
        require(number in MIN_FIELD_NUMBER..MAX_FIELD_NUMBER) {
            "$label must be in $MIN_FIELD_NUMBER..$MAX_FIELD_NUMBER, but was $number."
        }
        require(number !in FORBIDDEN_RANGE) {
            "$label must not be in reserved range $FORBIDDEN_RANGE, but was $number."
        }
    }
}
