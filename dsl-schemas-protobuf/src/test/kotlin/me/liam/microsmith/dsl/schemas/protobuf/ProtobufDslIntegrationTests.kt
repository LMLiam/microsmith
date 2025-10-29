package me.liam.microsmith.dsl.schemas.protobuf

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import me.liam.microsmith.dsl.schemas.core.SchemasBuilder
import me.liam.microsmith.dsl.schemas.protobuf.field.*
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange

class ProtobufDslIntegrationTests :
    StringSpec({
        "protobuf DSL builds message and enum schemas" {
            val schemasBuilder = SchemasBuilder()
            schemasBuilder.protobuf {
                message("Person") {
                    int32("id") { index(1) }
                    string("name") { index(2) }
                    optional { int64("age") { index(3) } }
                    repeated { string("tags") { index(4) } }
                    map("attrs") {
                        index(5)
                        key(PrimitiveType.STRING)
                        value(PrimitiveType.STRING)
                    }
                    oneof("choice") {
                        bool("active") { index(6) }
                        int32("score") { index(7) }
                    }
                    ref("color", "Color") { index(8) }
                    reserved {
                        index(9)
                        +(10..12)
                        +"LEGACY"
                    }
                }
                enum("Color") {
                    value("RED") { index(1) }
                    +"GREEN"
                    reserved(99)
                    reserved("OBSOLETE")
                }
            }

            val schemas = schemasBuilder.toExtension()
            val person = schemas.schemas.filterIsInstance<ProtobufMessageSchema>().first()
            val color = schemas.schemas.filterIsInstance<ProtobufEnumSchema>().first()

            person.name shouldBe "Person"
            person.message.name shouldBe "Person"
            person.message.fields.associate { it.name to it.index } shouldContainExactly
                mapOf(
                    "id" to 1,
                    "name" to 2,
                    "age" to 3,
                    "tags" to 4,
                    "attrs" to 5,
                    "color" to 8
                )
            person.message.fields
                .first { it.name == "age" }
                .let { it as ScalarField }
                .cardinality shouldBe Cardinality.OPTIONAL
            person.message.fields
                .first { it.name == "tags" }
                .let { it as ScalarField }
                .cardinality shouldBe Cardinality.REPEATED
            person.message.fields.first { it.name == "attrs" }.let { it as MapField }.type.also {
                it.key shouldBe PrimitiveType.STRING
                it.value shouldBe PrimitiveType.STRING
            }
            person.message.fields.first { it.name == "color" }.let { it as ReferenceField }.also {
                it.cardinality shouldBe Cardinality.REQUIRED
                it.reference.name shouldBe "Color"
                it.reference.type shouldBe color.enum
            }
            person.message.oneofs.first { it.name == "choice" }.fields.also { fields ->
                fields.associate { it.name to it.index } shouldContainExactly
                    mapOf(
                        "active" to 6,
                        "score" to 7
                    )
                fields.first { it.name == "active" }.fieldType shouldBe PrimitiveType.BOOL
                fields.first { it.name == "score" }.fieldType shouldBe PrimitiveType.INT32
            }
            person.message.reserved.filterIsInstance<ReservedRange>().also {
                it.size shouldBe 1
                it.first().indexRange shouldBe 9..12
            }
            person.message.reserved
                .filterIsInstance<ReservedName>()
                .first()
                .name shouldBe "LEGACY"

            color.name shouldBe "Color"
            color.enum.name shouldBe "Color"
            color.enum.values.associate { it.name to it.index } shouldContainExactly
                mapOf(
                    "UNSPECIFIED" to 0,
                    "RED" to 1,
                    "GREEN" to 2
                )
            color.enum.reserved
                .filterIsInstance<ReservedIndex>()
                .first()
                .index shouldBe 99
            color.enum.reserved
                .filterIsInstance<ReservedName>()
                .first()
                .name shouldBe "OBSOLETE"
        }

        "nested namespaces and versioning produce qualified names" {
            val schemasBuilder = SchemasBuilder()
            schemasBuilder.protobuf {
                "pkg.sub" {
                    message("Foo") {
                        int32("id")
                    }
                    2 {
                        enum("Status") {
                            +"OK"
                        }
                    }
                }
            }
            val schemas = schemasBuilder.toExtension()
            schemas.schemas.map { it.name } shouldContainExactlyInAnyOrder
                listOf(
                    "pkg.sub.Foo",
                    "pkg.sub.v2.Status"
                )
        }
    })