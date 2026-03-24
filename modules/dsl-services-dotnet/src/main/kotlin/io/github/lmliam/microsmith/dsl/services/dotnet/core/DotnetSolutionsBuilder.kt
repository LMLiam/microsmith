package io.github.lmliam.microsmith.dsl.services.dotnet.core

internal class DotnetSolutionsBuilder : DotnetSolutionsScope {
    private val solutionsByName = linkedMapOf<String, DotnetSolution>()

    override fun String.invoke(block: DotnetSolutionScope.() -> Unit) {
        val builder = DotnetSolutionBuilder(this).apply(block)
        register(builder.build())
    }

    fun build(): Map<String, DotnetSolution> = solutionsByName.toMap()

    private fun register(solution: DotnetSolution) {
        require(solution.name !in solutionsByName) {
            "Duplicate .NET solution registration for '${solution.name}'."
        }

        solutionsByName[solution.name] = solution
    }
}
