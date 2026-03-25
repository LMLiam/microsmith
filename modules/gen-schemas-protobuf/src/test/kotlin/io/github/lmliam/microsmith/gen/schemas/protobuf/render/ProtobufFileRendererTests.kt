package io.github.lmliam.microsmith.gen.schemas.protobuf.render

import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileArtifactId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path

class ProtobufFileRendererTests :
    StringSpec({
        "render sorts imports and renders package when present" {
            val rendered =
                ProtobufFileRenderer.render(
                    ProtoFileArtifact(
                        id = ProtoFileArtifactId(relativePath = Path.of("proto/pkg/User.proto")),
                        packageName = "pkg",
                        imports = listOf("alpha/A.proto", "zeta/Z.proto"),
                        declarations = listOf(ProtoDeclaration("User", "message User {}")),
                    ),
                )

            rendered.lineSequence().filter { it.startsWith("import") }.toList() shouldContainExactly
                listOf(
                    "import \"alpha/A.proto\";",
                    "import \"zeta/Z.proto\";",
                )
            rendered.shouldContain("package pkg;")
        }

        "render omits package when schema name is unqualified" {
            val rendered =
                ProtobufFileRenderer.render(
                    ProtoFileArtifact(
                        id = ProtoFileArtifactId(relativePath = Path.of("proto/User.proto")),
                        packageName = null,
                        imports = emptyList(),
                        declarations = listOf(ProtoDeclaration("User", "message User {}")),
                    ),
                )

            rendered.shouldNotContain("package ")
        }
    })
