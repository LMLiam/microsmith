package io.github.lmliam.microsmith.build.layers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.github.lmliam.microsmith.build.layers.ModuleLayerViolation.MetroTypeInPublishedApi
import io.github.lmliam.microsmith.build.layers.ModuleLayerViolation.PluginApiDependsOnRuntimeScripting
import io.github.lmliam.microsmith.build.layers.ModuleLayerViolation.UpwardDependency
import io.github.lmliam.microsmith.build.layers.PipelineLayer.ARTIFACT
import io.github.lmliam.microsmith.build.layers.PipelineLayer.COMPILE
import io.github.lmliam.microsmith.build.layers.PipelineLayer.DSL
import io.github.lmliam.microsmith.build.layers.PipelineLayer.GEN
import io.github.lmliam.microsmith.build.layers.PipelineLayer.PLUGIN_API
import io.github.lmliam.microsmith.build.layers.PipelineLayer.RESOLVE

private fun upward(
    module: String,
    moduleLayer: PipelineLayer,
    dependency: String,
    dependencyLayer: PipelineLayer,
) = UpwardDependency(module, moduleLayer, dependency, dependencyLayer)

private fun metro(
    module: String,
    type: String,
) = MetroTypeInPublishedApi(module, JvmClassName(type))

private val metroProviderReference = listOf("\tpublic final fun f (Ldev/zacsweers/metro/Provider;)V")

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

            violations.shouldContainExactly(upward("dsl-schemas", DSL, "resolve", RESOLVE))
        }

        "rejects a resolve module depending on an artifact module" {
            val violations =
                findModuleLayerViolations(
                    mapOf("resolve" to listOf("artifact-schemas")),
                    abiDumps = emptyMap(),
                )

            violations.shouldContainExactly(upward("resolve", RESOLVE, "artifact-schemas", ARTIFACT))
        }

        "rejects an artifact module depending on a compile module" {
            val violations =
                findModuleLayerViolations(
                    mapOf("artifact-services" to listOf("compile-services")),
                    abiDumps = emptyMap(),
                )

            violations.shouldContainExactly(upward("artifact-services", ARTIFACT, "compile-services", COMPILE))
        }

        "rejects a compile module depending on gen" {
            val violations =
                findModuleLayerViolations(
                    mapOf("compile" to listOf("gen")),
                    abiDumps = emptyMap(),
                )

            violations.shouldContainExactly(upward("compile", COMPILE, "gen", GEN))
        }

        "rejects plugin-api depending on gen" {
            val violations =
                findModuleLayerViolations(
                    mapOf("plugin-api" to listOf("gen", "compile")),
                    abiDumps = emptyMap(),
                )

            violations.shouldContainExactly(upward("plugin-api", PLUGIN_API, "gen", GEN))
        }

        "rejects compile depending on plugin-api" {
            val violations =
                findModuleLayerViolations(
                    mapOf("compile" to listOf("plugin-api")),
                    abiDumps = emptyMap(),
                )

            violations.shouldContainExactly(upward("compile", COMPILE, "plugin-api", PLUGIN_API))
        }

        "allows gen depending on plugin-api" {
            findModuleLayerViolations(mapOf("gen" to listOf("plugin-api", "compile")), abiDumps = emptyMap())
                .shouldBeEmpty()
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
                upward("dsl", DSL, "compile", COMPILE),
                upward("resolve", RESOLVE, "artifact", ARTIFACT),
                upward("resolve", RESOLVE, "gen", GEN),
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

        "rejects plugin-api depending on runtime-scripting" {
            val violations =
                findModuleLayerViolations(mapOf("plugin-api" to listOf("runtime-scripting")), abiDumps = emptyMap())

            violations.shouldContainExactly(PluginApiDependsOnRuntimeScripting("plugin-api", "runtime-scripting"))
        }

        "rejects plugin-api depending on runtime-scripting-api" {
            val violations =
                findModuleLayerViolations(mapOf("plugin-api" to listOf("runtime-scripting-api")), abiDumps = emptyMap())

            violations.shouldContainExactly(PluginApiDependsOnRuntimeScripting("plugin-api", "runtime-scripting-api"))
        }

        "allows modules other than plugin-api to depend on runtime scripting" {
            val dependencies = mapOf("gen" to listOf("runtime-scripting", "runtime-scripting-api"))

            findModuleLayerViolations(dependencies, abiDumps = emptyMap()).shouldBeEmpty()
        }

        "accepts a published API dump without a Metro type" {
            val dump = listOf("public final class io/github/lmliam/microsmith/dsl/Foo {", "}")

            findModuleLayerViolations(emptyMap(), abiDumps = mapOf("dsl" to dump)).shouldBeEmpty()
        }

        "rejects a published API that returns a Metro type" {
            val dump =
                listOf(
                    "public abstract interface class io/github/lmliam/microsmith/plugin/Host {",
                    "\tpublic abstract fun graph ()Ldev/zacsweers/metro/Provider;",
                    "}",
                )

            val violations = findModuleLayerViolations(emptyMap(), abiDumps = mapOf("plugin-api" to dump))

            violations.shouldContainExactly(metro("plugin-api", "dev/zacsweers/metro/Provider"))
        }

        "rejects a published API class that extends a Metro type" {
            val dump = listOf("public abstract interface class a/Host : dev/zacsweers/metro/Provider {", "}")

            val violations = findModuleLayerViolations(emptyMap(), abiDumps = mapOf("dsl" to dump))

            violations.shouldContainExactly(metro("dsl", "dev/zacsweers/metro/Provider"))
        }

        "rejects a Metro type from a subpackage of Metro" {
            val dump = listOf("\tpublic final fun f ()Ldev/zacsweers/metro/internal/Hidden;")

            val violations = findModuleLayerViolations(emptyMap(), abiDumps = mapOf("dsl" to dump))

            violations.shouldContainExactly(metro("dsl", "dev/zacsweers/metro/internal/Hidden"))
        }

        "does not reject a type from a package that merely resembles Metro" {
            val dump = listOf("\tpublic final fun f ()Ldev/zacsweers/metrology/Gauge;")

            findModuleLayerViolations(emptyMap(), abiDumps = mapOf("dsl" to dump)).shouldBeEmpty()
        }

        "reports each Metro type of a module once, in a stable order" {
            val dump =
                listOf(
                    "\tpublic final fun b (Ldev/zacsweers/metro/Provider;)V",
                    "\tpublic final fun a (Ldev/zacsweers/metro/Binds;Ldev/zacsweers/metro/Provider;)V",
                )

            val violations = findModuleLayerViolations(emptyMap(), abiDumps = mapOf("dsl" to dump))

            violations.shouldContainExactly(
                metro("dsl", "dev/zacsweers/metro/Binds"),
                metro("dsl", "dev/zacsweers/metro/Provider"),
            )
        }

        "rejects a Metro type in the dump of a layer submodule" {
            val violations =
                findModuleLayerViolations(
                    emptyMap(),
                    abiDumps = mapOf("compile-services-dotnet" to metroProviderReference),
                )

            violations.shouldContainExactly(metro("compile-services-dotnet", "dev/zacsweers/metro/Provider"))
        }

        "rejects a Metro type in the dump of runtime-scripting-api" {
            val violations =
                findModuleLayerViolations(
                    emptyMap(),
                    abiDumps = mapOf("runtime-scripting-api" to metroProviderReference),
                )

            violations.shouldContainExactly(metro("runtime-scripting-api", "dev/zacsweers/metro/Provider"))
        }

        "allows Metro in the dump of gen and of unpublished modules" {
            val dumps =
                mapOf(
                    "gen" to metroProviderReference,
                    "cli" to metroProviderReference,
                    "runtime-scripting" to metroProviderReference,
                )

            findModuleLayerViolations(emptyMap(), abiDumps = dumps).shouldBeEmpty()
        }
    })
