package io.github.lmliam.microsmith.artifact.schemas.protobuf.model

import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ProtoFileArtifactIdTests :
    StringSpec(
        {
            "a packaged file is imported by its package directory and type name" {
                ProtoFileArtifactId("acme.user.v1", "User").importPath shouldBe "acme/user/v1/User.proto"
            }

            "a file without a package is imported by its type name" {
                ProtoFileArtifactId(null, "User").importPath shouldBe "User.proto"
            }

            "a packaged file is written under the proto directory" {
                ProtoFileArtifactId("acme.user.v1", "User").relativePath shouldBe
                    RelativePath.of("proto", "acme", "user", "v1", "User.proto")
            }

            "a file without a package is written directly under the proto directory" {
                ProtoFileArtifactId(null, "User").relativePath shouldBe RelativePath.of("proto", "User.proto")
            }
        },
    )
