package me.liam.microsmith.cli.plugins

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import me.liam.microsmith.cli.command.RunCommand
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
class PluginResolverGuardrailTests :
    StringSpec({
        "returns success without evaluating credential resolvers when no plugins are requested" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-no-plugins")
            try {
                val command =
                    RunCommand(
                        script = tempDir.resolve("schema.microsmith.kts"),
                        outputDir = tempDir.resolve("generated"),
                    )
                val settings =
                    PluginResolverSettings(
                        cacheDirectory = tempDir.resolve("cache"),
                        repositoryCredentialsResolver =
                        object : RepositoryCredentialsResolver {
                            override fun resolve(repositoryUri: String): RepositoryCredentials {
                                error("resolve() should not be called when no plugins are requested.")
                            }

                            override fun sensitiveValues(): Set<String> {
                                error("sensitiveValues() should not be called when no plugins are requested.")
                            }
                        },
                    )

                val result = resolvePlugins(command = command, settings = settings)
                val success = result.shouldBeTypeOf<PluginResolutionResult.Success>()
                success.classpath.shouldBeEmpty()
                success.lockfilePath shouldBe null
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
