package io.github.lmliam.microsmith.dsl.services.dotnet.core.model

internal class DotnetModelsBuilder : DotnetModelsScope {
    private val modelsByName = linkedMapOf<String, DotnetModel>()

    override fun String.invoke(block: DotnetModelScope.() -> Unit) {
        model(this, block)
    }

    override fun model(name: String, block: DotnetModelScope.() -> Unit) {
        val builder = DotnetModelBuilder(name).apply(block)
        register(builder.build())
    }

    fun build(): Map<String, DotnetModel> = modelsByName.toMap()

    private fun register(model: DotnetModel) {
        require(model.name !in modelsByName) {
            "Duplicate .NET model registration for '${model.name}'."
        }

        modelsByName[model.name] = model
    }
}
