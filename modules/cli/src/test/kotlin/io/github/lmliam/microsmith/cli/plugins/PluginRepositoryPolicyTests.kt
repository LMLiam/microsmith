package io.github.lmliam.microsmith.cli.plugins

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PluginRepositoryPolicyTests :
    StringSpec({
        "default policy blocks file repositories" {
            val policy =
                defaultRepositoryAllowlistPolicy(
                    repositoryAllowlistEnv = null,
                    allowFileRepositoriesEnv = null,
                )

            val error =
                shouldThrow<IllegalArgumentException> {
                    policy.validate("file:///tmp/repo")
                }
            error.message.shouldBe(
                "Repository 'file:///tmp/repo' is blocked by policy: file:// repositories are not allowed. " +
                    "Set MICROSMITH_ALLOW_FILE_REPOSITORIES=true to explicitly enable file repositories.",
            )
        }

        "default policy allows file repositories when env is true" {
            val policy =
                defaultRepositoryAllowlistPolicy(
                    repositoryAllowlistEnv = null,
                    allowFileRepositoriesEnv = "true",
                )

            policy.validate("file:///tmp/repo")
        }

        "normalize rejects file repository authority host component" {
            val error =
                shouldThrow<IllegalArgumentException> {
                    normalizeRepositoryUri("file://server/share/repository")
                }

            error.message.shouldBe(
                "Repository URI 'file://server/share/repository' must not include " +
                    "an authority/host for file:// scheme.",
            )
        }

        "normalize rejects embedded credentials for http repositories" {
            val error =
                shouldThrow<IllegalArgumentException> {
                    normalizeRepositoryUri("https://user:pass@repo1.maven.org/maven2")
                }

            error.message.shouldBe(
                "Repository URI 'https://user:pass@repo1.maven.org/maven2' must not include userinfo credentials.",
            )
        }
    })
