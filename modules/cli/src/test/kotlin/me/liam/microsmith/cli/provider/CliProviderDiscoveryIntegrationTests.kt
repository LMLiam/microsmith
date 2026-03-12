package me.liam.microsmith.cli.provider

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CliProviderDiscoveryIntegrationTests :
    StringSpec({
        "discovers built-in providers through ServiceLoader" {
            verifyBuiltinProviders() shouldBe emptyList()
        }
    })
