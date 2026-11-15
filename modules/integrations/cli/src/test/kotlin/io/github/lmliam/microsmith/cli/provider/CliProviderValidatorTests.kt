package io.github.lmliam.microsmith.cli.provider

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class CliProviderValidatorTests :
    StringSpec(
        {
            "reports a required plugin id that is not present" {
                val errors =
                    verifyBuiltinProviders(
                        pluginIds = setOf("io.example.present"),
                        requiredPluginIds = listOf("io.example.present", "io.example.absent"),
                    )

                errors shouldContainExactly
                    listOf("Missing built-in plugin 'io.example.absent'. Check CLI runtime packaging.")
            }

            "reports no plugin error when every required plugin id is present" {
                val errors =
                    verifyBuiltinProviders(
                        pluginIds = setOf("io.example.present"),
                        requiredPluginIds = listOf("io.example.present"),
                    )

                errors shouldBe emptyList()
            }
        },
    )
