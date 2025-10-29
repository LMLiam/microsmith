package me.liam.microsmith.dsl.schemas.protobuf.types

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import me.liam.microsmith.dsl.schemas.protobuf.field.Cardinality
import me.liam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarField
import me.liam.microsmith.dsl.schemas.protobuf.reserved.MaxRange
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange

class MessageBuilderTests : StringSpec({
    fun builder(segments: List<String> = listOf("pkg", "sub")) =
        MessageBuilder(name = "Msg", segments = segments)

    "build returns deterministic sorted fields and oneofs, with reserved collected" {
        val b = builder()
        b.int32("a") { index(3) }
        b.int32("b") { index(1) }
        b.int32("c") { index(2) }
        b.oneof("beta") {
            int32("x") { index(20) }
        }
        b.oneof("alpha") {
            int32("y") { index(10) }
        }
        b.reserved(100..102)
        b.reserved("RES1", "RES2")

        val msg = b.build()
        msg.name shouldBe "Msg"
        msg.fields.map { it.name } shouldContainExactly listOf("b", "c", "a")
        msg.fields.map { it.index }.apply {
            this[0] shouldBe 1
            this[1] shouldBe 2
            this[2] shouldBe 3
        } shouldContainExactly listOf(1, 2, 3)
        msg.oneofs.map { it.name } shouldContainExactly listOf("alpha", "beta")
        msg.oneofs.flatMap { it.fields }.map { it.name } shouldContainExactly listOf("y", "x")
        msg.reserved shouldContainExactly listOf(
            ReservedRange(100..102),
            ReservedName("RES1"),
            ReservedName("RES2")
        )
    }

    "auto allocation skips proto reserved range and starts at 1" {
        val b = builder()
        for (i in 1..19001) {
            b.int32("field$i")
        }
        val msg = b.build()
        msg.fields.map { it.index }.apply {
            this[0] shouldBe 1 // starts at 1
            this[1] shouldBe 2
            this[18998] shouldBe 18999
            this[18999] shouldBe 20000 // skips proto reserved range
        }
    }

    "explicit index respected and does not change next auto allocation sequence" {
        val b = builder()
        b.int32("a") { index(5) }
        b.int32("b") // auto -> 1
        b.int32("c") // auto -> 2
        val msg = b.build()
        msg.fields.map { it.name } shouldContainExactly listOf("b", "c", "a")
        msg.fields.map { it.index }.apply {
            this[0] shouldBe 1
            this[1] shouldBe 2
            this[2] shouldBe 5
        } shouldContainExactly listOf(1, 2, 5)
    }

    "duplicate field names are rejected" {
        val b = builder()
        b.int32("a")
        shouldThrow<IllegalArgumentException>() {
            b.int32("a")
        }
    }

    "ref builds FQN via segments for unqualified target" {
        val b = builder(segments = listOf("me", "liam"))
        val f = b.ref("ref_field", "Person")
        f.name shouldBe "ref_field"
        f.index shouldBe 1
        f.reference.name shouldBe "me.liam.Person"
    }

    "ref respects qualified target ignoring segments" {
        val b = builder(segments = listOf("me", "liam"))
        val f = b.ref("ref_field", "me.someone.else.Person")
        f.name shouldBe "ref_field"
        f.index shouldBe 1
        f.reference.name shouldBe "me.someone.else.Person"
    }

    "ref supports relative target with leading dots dropping segments" {
        val b = builder(segments = listOf("a", "b", "c"))
        val f1 = b.ref("r1", ".Root")
        f1.reference.name shouldBe "a.b.Root"
        val f2 = b.ref("r2", ".x.Sub")
        f2.reference.name shouldBe "a.b.x.Sub"
        val f3 = b.ref("r3", "..Root")
        f3.reference.name shouldBe "a.Root"
        val f4 = b.ref("r4", "...Y.Sub")
        f4.reference.name shouldBe "Y.Sub"
    }

    "map requires key and value, allocates index, stores field" {
        val b = builder()
        val mf = b.map("tags") {
            index(7)
            key(PrimitiveType.STRING)
            value(PrimitiveType.STRING)
        }
        mf.name shouldBe "tags"
        mf.index shouldBe 7
        mf.type.key shouldBe PrimitiveType.STRING
        mf.type.value shouldBe PrimitiveType.STRING

        val msg = b.build()
        msg.fields.map { it.name } shouldContainExactly listOf("tags")
        msg.fields.map { it.index } shouldContainExactly listOf(7)
    }

    "map missing key throws" {
        val b = builder()
        shouldThrow<IllegalArgumentException> {
            b.map("tags") {
                index(7)
                value(PrimitiveType.STRING)
            }
        }
    }

    "map missing value throws" {
        val b = builder()
        shouldThrow<IllegalArgumentException> {
            b.map("tags") {
                index(7)
                key(PrimitiveType.STRING)
            }
        }
    }

    "map rejects duplicate field name" {
        val b = builder()
        b.map("tags") {
            types(PrimitiveType.STRING, PrimitiveType.STRING)
            index(7)
        }
        shouldThrow<IllegalArgumentException> {
            b.map("tags") {
                types(PrimitiveType.STRING, PrimitiveType.STRING)
                index(5)
            }
        }
    }

    "map rejects blank field name" {
        val b = builder()
        shouldThrow<IllegalArgumentException> {
            b.map("") {
                index(5)
            }
        }
    }

    "optional on scalar field flips cardinality and stores updated copy" {
        val b = builder()
        val s = b.int32("age") { index(3) }
        b.optional(s)
        val msg = b.build()
        val field = msg.fields.first { it.name == "age" } as ScalarField
        field.cardinality shouldBe Cardinality.OPTIONAL
        s.cardinality shouldBe Cardinality.REQUIRED
    }

    "optional on reference field flips cardinality and stores updated copy" {
        val b = builder()
        val s = b.ref("ref_field", "Person") { index(3) }
        b.optional(s)
        val msg = b.build()
        val field = msg.fields.first { it.name == "ref_field" } as ReferenceField
        field.cardinality shouldBe Cardinality.OPTIONAL
        s.cardinality shouldBe Cardinality.REQUIRED
    }

    "optional(block) builds, flips cardinality, and stores" {
        val b = builder()
        b.optional(block = { int32("age") { index(3) }})
        val msg = b.build()
        val field = msg.fields.first { it.name == "age" } as ScalarField
        field.cardinality shouldBe Cardinality.OPTIONAL
        field.index shouldBe 3
    }

    "optional(blockRef) builds reference, flips cardinality, and stores" {
        val b = builder()
        b.optional(blockRef = { ref("ref_field", "Person") { index(3) }})
        val msg = b.build()
        val field = msg.fields.first { it.name == "ref_field" } as ReferenceField
        field.cardinality shouldBe Cardinality.OPTIONAL
        field.index shouldBe 3
        field.reference.name shouldBe "pkg.sub.Person"
    }

    "repeated on scalar field flips cardinlaity and stores updated copy" {
        val b = builder()
        val s = b.int32("age") { index(3) }
        b.repeated(s)
        val msg = b.build()
        val field = msg.fields.first { it.name == "age" } as ScalarField
        field.cardinality shouldBe Cardinality.REPEATED
        s.cardinality shouldBe Cardinality.REQUIRED
    }

    "repeated on reference field flips cardinality" {
        val b = builder()
        val s = b.ref("ref_field", "Person") { index(3) }
        b.repeated(s)
        val msg = b.build()
        val field = msg.fields.first { it.name == "ref_field" } as ReferenceField
        field.cardinality shouldBe Cardinality.REPEATED
        s.cardinality shouldBe Cardinality.REQUIRED
    }

    "repeated(block) builds scalar, flips cardinality" {
        val b = builder()
        b.repeated(block = { int32("age") { index(3) }})
        val msg = b.build()
        val field = msg.fields.first { it.name == "age" } as ScalarField
        field.cardinality shouldBe Cardinality.REPEATED
        field.index shouldBe 3
    }

    "repeated(blockRef) builds reference, flips cardinality" {
        val b = builder()
        b.repeated(blockRef = { ref("ref_field", "Person") { index(3) }})
        val msg = b.build()
        val field = msg.fields.first { it.name == "ref_field" } as ReferenceField
        field.cardinality shouldBe Cardinality.REPEATED
        field.index shouldBe 3
        field.reference.name shouldBe "pkg.sub.Person"
    }

   "oneof builds and adds to message" {
       val b = builder()
       b.oneof("choice") {
           int32("optA") { index(33) }
           int32("optB") { index(34) }
       }
       val msg = b.build()
       msg.oneofs.map { it.name } shouldContainExactly listOf("choice")
       msg.oneofs.first().fields.map { it.name } shouldContainExactly listOf("optA", "optB")
   }

    "oneof rejects duplicate field names inside oneof" {
        val b = builder()
        shouldThrow<IllegalArgumentException> {
            b.oneof("dup") {
                int32("same") { index(1) }
                int32("same") { index(2) }
            }
        }
    }

    "reserved indexes prevent allocation of those indexes" {
        val b = builder()
        b.reserved(1)
        shouldThrow<IllegalArgumentException> {
            b.int32("foo") { index(1) }
        }
    }

    "reserved ranges prevent allocation within range" {
        val b = builder()
        b.reserved(1..3)
        shouldThrow<IllegalArgumentException> {
            b.int32("foo") { index(2) }
        }
    }

    "reserved toMax prevents allocation above threshold" {
        val b = builder()
        b.reserved(MaxRange(100))
        shouldThrow<IllegalArgumentException> {
            b.int32("foo") { index(101) }
        }
    }

    "reserved names prevent using same name for fields" {
        val b = builder()
        b.reserved("FOO")
        shouldThrow<IllegalArgumentException> {
            b.int32("FOO")
        }
    }

    "all scalar primitives construct fields and allocate indexes" {
        val b = builder()
        b.int32("i32")
        b.int64("i64")
        b.uint32("u32")
        b.uint64("u64")
        b.sint32("s32")
        b.sint64("s64")
        b.fixed32("f32")
        b.fixed64("f64")
        b.sfixed32("sf32")
        b.sfixed64("sf64")
        b.float("float")
        b.double("double")
        b.bytes("bytes")
        b.bool("bool")
        b.string("string")
        val msg = b.build()
        msg.fields.size shouldBe 15
        msg.fields.map { it.index } shouldContainExactly (1..15).toList()
    }
})