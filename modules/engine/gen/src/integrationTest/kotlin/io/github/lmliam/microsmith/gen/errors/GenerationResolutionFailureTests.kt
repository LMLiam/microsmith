package io.github.lmliam.microsmith.gen.errors

import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import io.github.lmliam.microsmith.gen.ServiceProviderClassLoader
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.github.lmliam.microsmith.gen.generate
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.ResolutionIssue
import io.github.lmliam.microsmith.resolve.ResolvedModel
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.nio.file.Files

data object FailureTestExtension : MicrosmithExtension

data object FailureTestResolvedModel : ResolvedModel

data object FailureTestIssue : ResolutionIssue

class FailingTestResolver : DomainResolver<FailureTestExtension, FailureTestResolvedModel> {
    override val authoringType = FailureTestExtension::class
    override val resolvedType = FailureTestResolvedModel::class

    override fun resolve(authoring: FailureTestExtension): DomainResolution<FailureTestResolvedModel> =
        DomainResolution.Failure(nonEmptyListOf(FailureTestIssue))
}

class FailureTestIssueDiagnosticMapper : ResolutionIssueDiagnosticMapper<FailureTestIssue> {
    override val issueType = FailureTestIssue::class

    override fun map(issue: FailureTestIssue): ResolutionDiagnostic =
        ResolutionDiagnostic(
            code = "test.resolution-failure",
            message = "Test resolution failed.",
        )
}

class GenerationResolutionFailureTests :
    StringSpec(
        {
            "generation maps semantic issues before exposing resolution failure" {
                val pluginClassLoader =
                    ServiceProviderClassLoader(
                        parent = GenerationResolutionFailureTests::class.java.classLoader,
                        registrations =
                            mapOf(
                                DomainResolver::class to listOf(FailingTestResolver::class),
                                ResolutionIssueDiagnosticMapper::class to
                                    listOf(FailureTestIssueDiagnosticMapper::class),
                            ),
                    )

                val model = MicrosmithBuilder().apply { put(FailureTestExtension::class, FailureTestExtension) }.model

                val output = DirectorySpace(Files.createTempDirectory("microsmith-resolution-failure"))

                val failure =
                    shouldThrow<GenerationResolutionFailedException> { model.generate(output, pluginClassLoader) }

                failure.issues.toList() shouldContainExactly listOf(FailureTestIssue)

                failure.diagnostics.toList() shouldContainExactly
                    listOf(
                        ResolutionDiagnostic(
                            code = "test.resolution-failure",
                            message = "Test resolution failed.",
                        ),
                    )

                failure.message shouldBe
                    "Microsmith model resolution failed with 1 semantic issue(s): " +
                        "[test.resolution-failure] Test resolution failed."
            }
        },
    )
