package io.github.lmliam.microsmith.dsl.schemas.protobuf.types

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class MessageTests :
    StringSpec({
        "message fields contain only legal message field types" {
            val fields =
                listOf(
                    ScalarField(
                        name = "id",
                        index = 1,
                        primitive = PrimitiveType.INT64,
                    ),
                    ReferenceField(
                        name = "owner",
                        index = 2,
                        reference = Reference.Qualified("Owner"),
                    ),
                    MapField(
                        name = "metadata",
                        index = 3,
                        type =
                            MapType(
                                key = PrimitiveType.STRING,
                                value = PrimitiveType.STRING,
                            ),
                    ),
                )

            Message(
                    name = "Thing",
                    fields = fields,
                )
                .fields shouldContainExactly fields
        }
    })
