package io.github.lmliam.microsmith.compile.services.dotnet.asp

internal object DotnetAspCSharpTypes {
    object System {
        data object InvalidOperationException : DotnetAspCSharpTypeName {
            override val value = "InvalidOperationException"
        }
    }

    object Primitives {
        data object String : DotnetAspCSharpTypeName {
            override val value = "string"
        }

        data object Int : DotnetAspCSharpTypeName {
            override val value = "int"
        }

        data object Object : DotnetAspCSharpTypeName {
            override val value = "object"
        }
    }

    object Threading {
        data object CancellationToken : DotnetAspCSharpTypeName {
            override val value = "CancellationToken"
        }

        data object Task : DotnetAspCSharpTypeName {
            override val value = "Task"
        }
    }

    object AspNetCore {
        object Mvc {
            data object ActionResult : DotnetAspCSharpTypeName {
                override val value = "ActionResult"
            }

            data object ControllerBase : DotnetAspCSharpTypeName {
                override val value = "ControllerBase"
            }

            data object ObjectResult : DotnetAspCSharpTypeName {
                override val value = "ObjectResult"
            }
        }

        object Builder {
            data object WebApplication : DotnetAspCSharpTypeName {
                override val value = "WebApplication"
            }

            data object WebApplicationBuilder : DotnetAspCSharpTypeName {
                override val value = "WebApplicationBuilder"
            }
        }
    }
}
