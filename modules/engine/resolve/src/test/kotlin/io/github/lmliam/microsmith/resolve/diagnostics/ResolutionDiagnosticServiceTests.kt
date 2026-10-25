package io.github.lmliam.microsmith.resolve.diagnostics

import io.github.lmliam.microsmith.resolve.ResolutionIssue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

private sealed interface TestIssue : ResolutionIssue {
    data object Alpha : TestIssue

    data object Beta : TestIssue
}

private class TestIssueMapper : ResolutionIssueDiagnosticMapper<TestIssue> {
    override val issueType = TestIssue::class

    override fun map(issue: TestIssue): ResolutionDiagnostic =
        when (issue) {
            TestIssue.Alpha ->
                ResolutionDiagnostic(
                    code = "test.alpha",
                    message = "Alpha failed",
                )

            TestIssue.Beta ->
                ResolutionDiagnostic(
                    code = "test.beta",
                    message = "Beta failed",
                )
        }
}

private class AlphaIssueMapper : ResolutionIssueDiagnosticMapper<TestIssue.Alpha> {
    override val issueType = TestIssue.Alpha::class

    override fun map(issue: TestIssue.Alpha): ResolutionDiagnostic =
        ResolutionDiagnostic(
            code = "test.alpha-specific",
            message = "Specific alpha failure",
        )
}

private class DuplicateTestIssueMapper : ResolutionIssueDiagnosticMapper<TestIssue> {
    override val issueType = TestIssue::class

    override fun map(issue: TestIssue): ResolutionDiagnostic =
        ResolutionDiagnostic(
            code = "test.duplicate",
            message = "Duplicate",
        )
}

private class IncorrectIssueTypeMapper : ResolutionIssueDiagnosticMapper<TestIssue.Alpha> {
    @Suppress("UNCHECKED_CAST") override val issueType = TestIssue.Beta::class as KClass<TestIssue.Alpha>

    override fun map(issue: TestIssue.Alpha): ResolutionDiagnostic =
        ResolutionDiagnostic(
            code = "test.invalid",
            message = "Invalid",
        )
}

private data object UnmappedIssue : ResolutionIssue

class ResolutionDiagnosticServiceTests :
    StringSpec({
        "describe maps subtype issues through a hierarchy mapper" {
            val service = ResolutionDiagnosticService(listOf(TestIssueMapper()))

            service.describe(TestIssue.Alpha) shouldBe
                ResolutionDiagnostic(
                    code = "test.alpha",
                    message = "Alpha failed",
                )
        }

        "describe prefers the most specific matching mapper" {
            val service =
                ResolutionDiagnosticService(
                    listOf(
                        TestIssueMapper(),
                        AlphaIssueMapper(),
                    )
                )

            service.describe(TestIssue.Alpha) shouldBe
                ResolutionDiagnostic(
                    code = "test.alpha-specific",
                    message = "Specific alpha failure",
                )

            service.describe(TestIssue.Beta) shouldBe
                ResolutionDiagnostic(
                    code = "test.beta",
                    message = "Beta failed",
                )
        }

        "describe returns a safe fallback for unmapped issues" {
            ResolutionDiagnosticService().describe(UnmappedIssue) shouldBe
                ResolutionDiagnostic(
                    code = "resolution.unmapped-issue",
                    message = "Resolution failed with issue type " + "'${UnmappedIssue::class.qualifiedName}'.",
                )
        }

        "registry rejects duplicate mapper issue types" {
            shouldThrow<IllegalArgumentException> {
                ResolutionDiagnosticService(
                    listOf(
                        TestIssueMapper(),
                        DuplicateTestIssueMapper(),
                    )
                )
            }
        }

        "registry rejects issueType declarations that disagree with the generic contract" {
            shouldThrow<IllegalArgumentException> { ResolutionDiagnosticService(listOf(IncorrectIssueTypeMapper())) }
        }
    })
