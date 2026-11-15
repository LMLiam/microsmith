package io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.oneof

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ScalarFields
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.ProtobufTypeRef
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.OneofFieldScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.OneofReferenceFieldScope

@MicrosmithDsl
public interface OneofScope : ScalarFields<OneofFieldScope, OneofField> {
    public fun ref(name: String, target: String, block: OneofReferenceFieldScope.() -> Unit = {}): OneofField

    public fun ref(name: String, target: ProtobufTypeRef, block: OneofReferenceFieldScope.() -> Unit = {}): OneofField
}
