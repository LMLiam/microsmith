package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetConfigurableTypedFieldScope

@MicrosmithDsl
interface DotnetAspRequestBindingScope :
    DotnetConfigurableTypedFieldScope<DotnetAspRequestField, DotnetAspRequestFieldScope>
