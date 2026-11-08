package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
interface ReferenceFieldScope :
    OneofReferenceFieldScope,
    ScalarFieldScope
