package io.github.lmliam.microsmith.compile.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.microsmith
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.service
import io.github.lmliam.microsmith.dsl.schemas.schemas
import io.github.lmliam.microsmith.gen.generateTo
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo
import kotlin.io.path.toPath
import kotlin.io.path.walk

class ProtobufGeneratedOutputIntegrationTests :
    StringSpec(
        {
            "generateTo protobuf and rpc models writes files byte-identical to the expected output files" {
                val model = microsmith {
                    schemas {
                        protobuf {
                            "acme.shared.v1" {
                                message("Address") { string("street") { index(1) } }
                                enum("Region") {
                                    value("NORTH") { index(1) }
                                    value("SOUTH") { index(2) }
                                    reserved(3, 4)
                                    reserved(10..12)
                                    reserved("LEGACY_REGION")
                                }
                            }
                            "acme.user.v1" {
                                enum("Level") {
                                    value("LOW") { index(1) }
                                    value("HIGH") { index(5) }
                                    reserved(2, 3)
                                    reserved(10..12)
                                    reserved("OLD_LEVEL")
                                }
                                message("Profile") {
                                    reserved(4)
                                    reserved(7..9)
                                    reserved("legacy_name")
                                    reserved(100..max)
                                    string("nickname") { optional() }
                                    int32("score")
                                    string("tags") { repeated() }
                                    map("attributes") {
                                        key(PrimitiveType.STRING)
                                        value(PrimitiveType.INT32)
                                    }
                                    map("addresses") {
                                        key(PrimitiveType.STRING)
                                        value("acme.shared.v1.Address")
                                    }
                                    map("ranks") {
                                        key(PrimitiveType.UINT64)
                                        value(PrimitiveType.INT32)
                                    }
                                    oneof("contact") {
                                        string("email")
                                        ref("backup", "acme.shared.v1.Address")
                                    }
                                }
                                message("User") {
                                    int64("id")
                                    uint64("count")
                                    sint32("delta")
                                    fixed32("checksum")
                                    sfixed64("offset")
                                    uint32("shard")
                                    sint64("drift")
                                    fixed64("digest")
                                    sfixed32("bias")
                                    double("weight")
                                    float("ratio")
                                    bool("active")
                                    bytes("avatar")
                                    ref("profile", "Profile")
                                    ref("home", "acme.shared.v1.Address")
                                    ref("region", "acme.shared.v1.Region")
                                    ref("level", "Level")
                                    ref("peers", "User") { repeated() }
                                }
                                message("GetUserRequest")
                                message("GetUserResponse")
                                message("WatchUsersRequest")
                                message("ImportUsersResponse")
                                message("ChatRequest")
                                message("ChatResponse")
                                service("UserService") {
                                    "GetUser" { "GetUserRequest" to "GetUserResponse" }
                                    "WatchUsers" { "WatchUsersRequest" to stream("User") }
                                    "ImportUsers" { stream("User") to "ImportUsersResponse" }
                                    "Chat" {
                                        request("ChatRequest") { stream() }
                                        response("ChatResponse")
                                    }
                                }
                            }
                            "acme.audit.v1" {
                                message("AuditEvent") { ref("subject", "acme.user.v1.User") }
                                service("AuditService") {
                                    "RecordEvents" { stream("acme.user.v1.User") to stream("AuditEvent") }
                                }
                            }
                            message("GlobalEnvelope") { ref("payload", "acme.user.v1.User") }
                        }
                    }
                }

                val outputRoot = tempdir("generated-output-").toPath()
                model.generateTo(outputRoot)

                val generated = outputRoot.regularFilesByRelativePath()
                val expected =
                    checkNotNull(javaClass.getResource("/expected-output/protobuf"))
                        .toURI()
                        .toPath()
                        .regularFilesByRelativePath()

                generated.keys shouldBe expected.keys
                generated.filter { (path, bytes) -> !bytes.contentEquals(expected.getValue(path)) }.keys shouldBe
                    emptySet()
            }
        },
    )

private fun Path.regularFilesByRelativePath(): Map<String, ByteArray> =
    walk()
        .filter { it.isRegularFile() }
        .associate { file -> file.relativeTo(this).invariantSeparatorsPathString to file.readBytes() }
