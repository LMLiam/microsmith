package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpTypeName

internal object DotnetAspCSharpTypes {
    object AspNetCore {
        object Mvc {
            data object ActionResult : DotnetCSharpTypeName {
                override val value = "ActionResult"
            }

            data object ControllerBase : DotnetCSharpTypeName {
                override val value = "ControllerBase"
            }

            data object ObjectResult : DotnetCSharpTypeName {
                override val value = "ObjectResult"
            }
        }

        object Builder {
            data object WebApplication : DotnetCSharpTypeName {
                override val value = "WebApplication"
            }

            data object WebApplicationBuilder : DotnetCSharpTypeName {
                override val value = "WebApplicationBuilder"
            }
        }
    }
}
