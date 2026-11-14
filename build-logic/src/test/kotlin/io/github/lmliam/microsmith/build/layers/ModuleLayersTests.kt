package io.github.lmliam.microsmith.build.layers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly

class ModuleLayersTests :
    StringSpec({

        "accepts a module depending on its own layer and on lower layers" {
            val violations =
                findModuleLayerViolations(
                    mapOf(
                        "artifact-schemas-protobuf" to listOf("artifact-schemas", "resolve-schemas-protobuf", "dsl"),
                        "gen" to listOf("dsl", "resolve", "artifact", "compile"),
                    ),
                    metroModules = emptyList(),
                )

            violations.shouldBeEmpty()
        }

        "rejects a dsl module depending on a resolve module" {
            val violations =
                findModuleLayerViolations(
                    mapOf("dsl-schemas" to listOf("resolve")),
                    metroModules = emptyList(),
                )

            violations.shouldContainExactly("dsl-schemas (dsl) -> resolve (resolve)")
        }

        "rejects a resolve module depending on an artifact module" {
            val violations =
                findModuleLayerViolations(
                    mapOf("resolve" to listOf("artifact-schemas")),
                    metroModules = emptyList(),
                )

            violations.shouldContainExactly("resolve (resolve) -> artifact-schemas (artifact)")
        }

        "rejects an artifact module depending on a compile module" {
            val violations =
                findModuleLayerViolations(
                    mapOf("artifact-services" to listOf("compile-services")),
                    metroModules = emptyList(),
                )

            violations.shouldContainExactly("artifact-services (artifact) -> compile-services (compile)")
        }

        "rejects a compile module depending on gen" {
            val violations =
                findModuleLayerViolations(
                    mapOf("compile" to listOf("gen")),
                    metroModules = emptyList(),
                )

            violations.shouldContainExactly("compile (compile) -> gen (gen)")
        }

        "allows downstream modules to reuse dsl vocabulary" {
            val violations =
                findModuleLayerViolations(
                    mapOf(
                        "artifact-schemas-protobuf" to listOf("dsl-schemas-protobuf"),
                        "compile-schemas-protobuf" to listOf("dsl-schemas-protobuf"),
                    ),
                    metroModules = emptyList(),
                )

            violations.shouldBeEmpty()
        }

        "does not constrain modules outside the pipeline layers" {
            val violations =
                findModuleLayerViolations(
                    mapOf(
                        "cli" to listOf("gen", "dsl"),
                        "runtime-scripting" to listOf("compile"),
                        "dsl" to listOf("runtime-scripting-api"),
                    ),
                    metroModules = emptyList(),
                )

            violations.shouldBeEmpty()
        }

        "reports every violation in a stable order" {
            val violations =
                findModuleLayerViolations(
                    mapOf(
                        "resolve" to listOf("gen", "artifact"),
                        "dsl" to listOf("compile"),
                    ),
                    metroModules = emptyList(),
                )

            violations.shouldContainExactly(
                "dsl (dsl) -> compile (compile)",
                "resolve (resolve) -> artifact (artifact)",
                "resolve (resolve) -> gen (gen)",
            )
        }

        "does not treat a name that merely starts with a layer word as in that layer" {
            val violations =
                findModuleLayerViolations(
                    mapOf("dsl" to listOf("generator", "compiler-tools", "resolver")),
                    metroModules = emptyList(),
                )

            violations.shouldBeEmpty()
        }

        "accepts Metro in gen" {
            findModuleLayerViolations(emptyMap(), metroModules = listOf("gen")).shouldBeEmpty()
        }

        "rejects Metro outside gen" {
            val violations = findModuleLayerViolations(emptyMap(), metroModules = listOf("compile", "gen"))

            violations.shouldContainExactly("compile applies Metro; only gen may")
        }
    })
