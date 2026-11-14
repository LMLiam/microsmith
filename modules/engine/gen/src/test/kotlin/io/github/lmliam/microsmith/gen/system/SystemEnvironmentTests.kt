package io.github.lmliam.microsmith.gen.system

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SystemEnvironmentTests :
    StringSpec(
        {
            val environment = SystemEnvironment()

            "property returns the value of a JVM system property" {
                environment.property("java.io.tmpdir") shouldBe System.getProperty("java.io.tmpdir")
            }

            "property returns null for a property that is not set" {
                environment.property("microsmith.test.unset-property") shouldBe null
            }

            "variable returns the value of an environment variable" {
                val (name, value) = System.getenv().entries.first()

                environment.variable(name) shouldBe value
            }

            "variable returns null for a variable that is not set" {
                environment.variable("MICROSMITH_TEST_UNSET_VARIABLE") shouldBe null
            }
        },
    )
