package io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField

data class Oneof(val name: String, val fields: List<OneofField>)
