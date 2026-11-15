package io.github.lmliam.microsmith.runtime.scripting.host.configuration

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.github.lmliam.microsmith.runtime.scripting.TestResources
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class ScriptDirectivePolicyTests :
    StringSpec(
        {
            "blocks a script that declares a dependency directive" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val script = fileSystem.getPath("/scripts/blocked.microsmith.kts")
                    script.parent.createDirectories()
                    script.writeText(
                        TestResources.read("/scripting/host/configuration/blocked-depends-on.microsmith.kts"),
                    )

                    val failure = ScriptDirectivePolicy().validate(script).shouldBeInstanceOf<ScriptRunFailure>()

                    failure.type shouldBe ScriptFailureType.VALIDATION
                }
            }

            "accepts a script without dependency directives" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val script = fileSystem.getPath("/scripts/allowed.microsmith.kts")
                    script.parent.createDirectories()
                    script.writeText("microsmith { }")

                    ScriptDirectivePolicy().validate(script) shouldBe null
                }
            }
        },
    )
