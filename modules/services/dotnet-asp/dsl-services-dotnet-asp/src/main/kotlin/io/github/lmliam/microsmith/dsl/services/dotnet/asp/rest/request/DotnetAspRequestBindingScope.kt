package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetConfigurableTypedFieldScope

@MicrosmithDsl
public interface DotnetAspRequestBindingScope :
    DotnetConfigurableTypedFieldScope<DotnetAspRequestField, DotnetAspRequestFieldScope>
