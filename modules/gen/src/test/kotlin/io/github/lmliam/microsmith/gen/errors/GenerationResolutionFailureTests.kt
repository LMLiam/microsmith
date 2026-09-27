package io.github.lmliam.microsmith.gen.errors

import arrow.core.nonEmptyListOf
import dev.zacsweers.metro.createGraphFactory
import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import io.github.lmliam.microsmith.gen.composition.MicrosmithGenerationGraph
import io.github.lmliam.microsmith.gen.errors.GenerationResolutionFailedException
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.github.lmliam.microsmith.gen.plugins.MicrosmithPluginCatalog
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

private data object TestExtension : MicrosmithExtension

private data object TestResolvedModel : ResolvedModel

private data object TestIssue : ResolutionIssue

private class FailingTestResolver : DomainResolver<TestExtension, TestResolvedModel> {
    override val authoringType = TestExtension::class
    override val resolvedType = TestResolvedModel::class

    override fun resolve(authoring: TestExtension): DomainResolution<TestResolvedModel> = DomainResolution.Failure(
        nonEmptyListOf(TestIssue),
    )
}

private class TestIssueDiagnosticMapper : ResolutionIssueDiagnosticMapper<TestIssue> {
    override val issueType = TestIssue::class

    override fun map(issue: TestIssue): ResolutionDiagnostic = ResolutionDiagnostic(
        code = "test.resolution-failure",
        message = "Test resolution failed.",
    )
}

class GenerationResolutionFailureTests :
    StringSpec({
        "generation maps semantic issues before exposing resolution failure" {
            val pluginCatalog = MicrosmithPluginCatalog(
                domainResolvers = listOf(FailingTestResolver()),
                resolutionIssueDiagnosticMappers = listOf(TestIssueDiagnosticMapper()),
            )

            val graph = createGraphFactory<MicrosmithGenerationGraph.Factory>()
                .create(pluginCatalog)

            val model = MicrosmithBuilder().apply {
                put(TestExtension::class, TestExtension)
            }.model

            val output = DirectorySpace.from(Files.createTempDirectory("microsmith-resolution-failure"))

            val failure = shouldThrow<GenerationResolutionFailedException> {
                graph.runner.generate(model, output)
            }

            failure.issues.toList() shouldContainExactly listOf(TestIssue)

            failure.diagnostics.toList() shouldContainExactly listOf(
                ResolutionDiagnostic(
                    code = "test.resolution-failure",
                    message = "Test resolution failed.",
                ),
            )

            failure.message shouldBe "Microsmith model resolution failed with 1 semantic issue(s): " +
                "[test.resolution-failure] Test resolution failed."
        }
    })
