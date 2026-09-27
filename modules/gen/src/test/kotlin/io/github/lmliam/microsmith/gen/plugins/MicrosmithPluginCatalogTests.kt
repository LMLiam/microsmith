package io.github.lmliam.microsmith.gen.plugins

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain

class MicrosmithPluginCatalogTests :
    StringSpec({
        "plugin discovery loads resolution diagnostic mappers" {
            val catalog = discoverMicrosmithPlugins()

            catalog
                .resolutionIssueDiagnosticMappers
                .mapNotNull { it::class.qualifiedName } shouldContain
                "io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionDiagnosticMapper"
        }
    })
