package me.liam.microsmith.gen.schemas.protobuf.render

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.liam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName

class ProtobufFileRendererTests :
    StringSpec({
        "render sorts imports and renders package when present" {
            val rendered =
                ProtobufFileRenderer.render(
                    qualifiedName = QualifiedSchemaName.parse("pkg.User"),
                    declaration = "message User {}",
                    imports = listOf("zeta/Z.proto", "alpha/A.proto"),
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
                    qualifiedName = QualifiedSchemaName.parse("User"),
                    declaration = "message User {}",
                )

            rendered.shouldNotContain("package ")
        }
    })
