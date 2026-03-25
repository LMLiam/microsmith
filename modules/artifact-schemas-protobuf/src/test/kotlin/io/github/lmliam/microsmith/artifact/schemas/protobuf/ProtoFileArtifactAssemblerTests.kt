package io.github.lmliam.microsmith.artifact.schemas.protobuf

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class ProtoFileArtifactAssemblerTests :
    StringSpec({
        val assembler = ProtoFileArtifactAssembler()
        val sharedId = ProtoFileArtifactId(relativePath = Path.of("proto/acme/user/v1/User.proto"))

        "merges imports and declarations for the same proto artifact" {
            val assembled =
                assembler.merge(
                    assembler.create(
                        ProtoFileContribution(
                            artifactId = sharedId,
                            packageName = "acme.user.v1",
                            imports = listOf("acme/user/v1/Role.proto"),
                            declarations = listOf(ProtoDeclaration("User", "message User {}")),
                        ),
                    ),
                    ProtoFileContribution(
                        artifactId = sharedId,
                        packageName = "acme.user.v1",
                        imports = listOf("acme/user/v1/Profile.proto", "acme/user/v1/Role.proto"),
                        declarations = listOf(ProtoDeclaration("Profile", "message Profile {}")),
                    ),
                )

            assembled.imports shouldContainExactly
                listOf(
                    "acme/user/v1/Profile.proto",
                    "acme/user/v1/Role.proto",
                )
            assembled.declarations.map(ProtoDeclaration::name) shouldContainExactly listOf("User", "Profile")
        }

        "rejects conflicting declarations for the same top-level name" {
            val current =
                assembler.create(
                    ProtoFileContribution(
                        artifactId = sharedId,
                        packageName = "acme.user.v1",
                        declarations = listOf(ProtoDeclaration("User", "message User {}")),
                    ),
                )

            val error =
                shouldThrow<IllegalArgumentException> {
                    assembler.merge(
                        current,
                        ProtoFileContribution(
                            artifactId = sharedId,
                            packageName = "acme.user.v1",
                            declarations = listOf(ProtoDeclaration("User", "message User { string id = 1; }")),
                        ),
                    )
                }

            error.message shouldBe
                "Conflicting protobuf declaration 'User' for 'proto/acme/user/v1/User.proto' under '.'."
        }
    })
