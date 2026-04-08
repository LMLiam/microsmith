package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

internal class DotnetAspRequestFieldOptions : DotnetAspRequestFieldScope {
    var optional = false
        private set
    var defaultValue: DotnetAspDefaultValue? = null
        private set

    override fun optional() {
        require(!optional) { "optional() already set for ASP.NET request field." }
        optional = true
    }

    override fun default(value: Any) {
        require(defaultValue == null) { "default(...) already set for ASP.NET request field." }
        defaultValue = dotnetAspDefaultValue(value)
    }
}
