package io.github.lmliam.microsmith.runtime.scripting.host.paths

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.github.lmliam.microsmith.plugin.Environment
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private class FixedEnvironment(
    private val variables: Map<String, String> = emptyMap(),
    private val properties: Map<String, String> = emptyMap(),
) : Environment {
    override fun variable(name: String): String? = variables[name]

    override fun property(name: String): String? = properties[name]
}

class ScriptCacheDirectoryPolicyTests :
    StringSpec(
        {
            "the cache directory variable wins over the user home" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val policy =
                        ScriptCacheDirectoryPolicy(
                            environment =
                                FixedEnvironment(
                                    variables =
                                        mapOf(
                                            ScriptCacheDirectoryPolicy.CACHE_DIRECTORY_VARIABLE to "/var/cache/scripts",
                                        ),
                                    properties = mapOf(ScriptCacheDirectoryPolicy.USER_HOME_PROPERTY to "/home/user"),
                                ),
                            fileSystem = fileSystem,
                            configuredDirectory = null,
                        )

                    policy.resolve() shouldBe fileSystem.getPath("/var/cache/scripts")
                }
            }

            "a blank cache directory variable falls back to the user home" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val policy =
                        ScriptCacheDirectoryPolicy(
                            environment =
                                FixedEnvironment(
                                    variables = mapOf(ScriptCacheDirectoryPolicy.CACHE_DIRECTORY_VARIABLE to "  "),
                                    properties = mapOf(ScriptCacheDirectoryPolicy.USER_HOME_PROPERTY to "/home/user"),
                                ),
                            fileSystem = fileSystem,
                            configuredDirectory = null,
                        )

                    policy.resolve() shouldBe fileSystem.getPath("/home/user/.microsmith/cache/scripts")
                }
            }
        },
    )
