package io.github.lmliam.microsmith.dsl.services.dotnet.core.solution

internal class DotnetSolutionsBuilder : DotnetSolutionsScope {
    private val solutionsByName = linkedMapOf<String, DotnetSolution>()

    override fun String.invoke(block: DotnetSolutionScope.() -> Unit) {
        solution(this, block)
    }

    override fun solution(name: String, block: DotnetSolutionScope.() -> Unit) {
        val builder = DotnetSolutionBuilder(name).apply(block)
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
