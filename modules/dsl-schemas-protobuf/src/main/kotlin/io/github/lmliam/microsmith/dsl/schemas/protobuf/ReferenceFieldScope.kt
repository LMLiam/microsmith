package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface ReferenceFieldScope :
    OneofReferenceFieldScope,
    ScalarFieldScope
