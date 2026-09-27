package io.github.lmliam.microsmith.resolve.diagnostics

import io.github.lmliam.microsmith.resolve.ResolutionIssue
import kotlin.reflect.KClass

internal class ResolutionIssueDiagnosticMapperRegistry(mappers: List<ResolutionIssueDiagnosticMapper<*>>) {
    private val mappers = mappers
        .onEach(::validateIssueTypeDeclaration)
        .also(::requireDistinctIssueTypes)
        .sortedWith(
            compareBy(
                { it.issueType.displayName() },
                { it::class.displayName() },
            ),
        )

    fun resolve(issue: ResolutionIssue): ResolutionIssueDiagnosticMapper<ResolutionIssue>? {
        val issueClass = issue::class

        val candidates = mappers.filter { mapper -> mapper.issueType.java.isAssignableFrom(issueClass.java) }
        if (candidates.isEmpty()) return null

        val mostSpecific = candidates.filter { candidate ->
            candidates.none { other ->
                candidate !== other && candidate.issueType.java.isAssignableFrom(other.issueType.java)
            }
        }

        require(mostSpecific.size == 1) {
            val mapperNames = mostSpecific
                .map { it::class.displayName() }
                .sorted()
                .joinToString(", ")

            "Ambiguous resolution diagnostic mappers for " +
                "${issueClass.displayName()}: $mapperNames"
        }

        return mostSpecific.single().cast()
    }

    private fun validateIssueTypeDeclaration(mapper: ResolutionIssueDiagnosticMapper<*>) {
        val declaredType = mapper.issueType
        val genericType = mapper.findGenericIssueType()

        require(genericType == declaredType) {
            "${mapper::class.displayName()} declares issueType " +
                "${declaredType.displayName()}, but implements" +
                "ResolutionIssueDiagnosticMapper<${genericType.displayName()}>. " +
                "Ensure issueType matches the generic issue type."
        }
    }

    private fun requireDistinctIssueTypes(mappers: List<ResolutionIssueDiagnosticMapper<*>>) {
        val duplicates = mappers
            .groupBy(ResolutionIssueDiagnosticMapper<*>::issueType)
            .filterValues { it.size > 1 }

        require(duplicates.isEmpty()) {
            val registrations = duplicates
                .entries
                .sortedBy { it.key.displayName() }
                .joinToString("; ") { (issueType, issueMappers) ->
                    val mapperNames = issueMappers
                        .map { it::class.displayName() }
                        .sorted()
                        .joinToString(", ")

                    "${issueType.displayName()}: $mapperNames"
                }

            "Multiple resolution diagnostic mappers registered " +
                "for the same issue type: $registrations"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun ResolutionIssueDiagnosticMapper<*>.cast(): ResolutionIssueDiagnosticMapper<ResolutionIssue> =
        this as ResolutionIssueDiagnosticMapper<ResolutionIssue>
}

private fun KClass<*>.displayName(): String = qualifiedName ?: toString()
