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
                    abiDumps = emptyMap(),
                )

            violations.shouldBeEmpty()
        }

        "rejects a dsl module depending on a resolve module" {
            val violations =
                findModuleLayerViolations(
                    mapOf("dsl-schemas" to listOf("resolve")),
                    abiDumps = emptyMap(),
                )

            violations.shouldContainExactly("dsl-schemas (dsl) -> resolve (resolve)")
        }

        "rejects a resolve module depending on an artifact module" {
            val violations =
                findModuleLayerViolations(
                    mapOf("resolve" to listOf("artifact-schemas")),
                    abiDumps = emptyMap(),
                )

            violations.shouldContainExactly("resolve (resolve) -> artifact-schemas (artifact)")
        }

        "rejects an artifact module depending on a compile module" {
            val violations =
                findModuleLayerViolations(
                    mapOf("artifact-services" to listOf("compile-services")),
                    abiDumps = emptyMap(),
                )

            violations.shouldContainExactly("artifact-services (artifact) -> compile-services (compile)")
        }

        "rejects a compile module depending on gen" {
            val violations =
                findModuleLayerViolations(
                    mapOf("compile" to listOf("gen")),
                    abiDumps = emptyMap(),
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
                    abiDumps = emptyMap(),
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
                    abiDumps = emptyMap(),
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
                    abiDumps = emptyMap(),
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
                    abiDumps = emptyMap(),
                )

            violations.shouldBeEmpty()
        }

        "accepts a published API dump without a Metro type" {
            val dump = listOf("public final class io/github/lmliam/microsmith/dsl/Foo {", "}")

            findModuleLayerViolations(emptyMap(), abiDumps = mapOf("dsl" to dump)).shouldBeEmpty()
        }

        "rejects a published API dump that mentions a Metro type" {
            val dump =
                listOf(
                    "public abstract interface class io/github/lmliam/microsmith/plugin/Host {",
                    "\tpublic abstract fun graph ()Ldev/zacsweers/metro/Provider;",
                    "}",
                )

            val violations = findModuleLayerViolations(emptyMap(), abiDumps = mapOf("plugin-api" to dump))

            violations.shouldContainExactly(
                "plugin-api exposes a Metro type: public abstract fun graph ()Ldev/zacsweers/metro/Provider;",
            )
        }

        "rejects a Metro type in the dump of a layer submodule" {
            val leaky = listOf("public final fun f (Ldev/zacsweers/metro/Provider;)V")

            val violations =
                findModuleLayerViolations(emptyMap(), abiDumps = mapOf("compile-services-dotnet" to leaky))

            violations.shouldContainExactly(
                "compile-services-dotnet exposes a Metro type: public final fun f (Ldev/zacsweers/metro/Provider;)V",
            )
        }

        "rejects a Metro type in the dump of runtime-scripting-api" {
            val leaky = listOf("public final fun f (Ldev/zacsweers/metro/Provider;)V")

            val violations = findModuleLayerViolations(emptyMap(), abiDumps = mapOf("runtime-scripting-api" to leaky))

            violations.shouldContainExactly(
                "runtime-scripting-api exposes a Metro type: public final fun f (Ldev/zacsweers/metro/Provider;)V",
            )
        }

        "allows Metro in the dump of gen and of unpublished modules" {
            val leaky = listOf("public final fun f (Ldev/zacsweers/metro/Provider;)V")

            val dumps = mapOf("gen" to leaky, "cli" to leaky, "runtime-scripting" to leaky)

            findModuleLayerViolations(emptyMap(), abiDumps = dumps).shouldBeEmpty()
        }
    })
