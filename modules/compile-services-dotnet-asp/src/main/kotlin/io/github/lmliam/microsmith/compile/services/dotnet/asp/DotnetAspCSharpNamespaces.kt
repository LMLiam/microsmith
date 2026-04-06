package io.github.lmliam.microsmith.compile.services.dotnet.asp

internal object DotnetAspCSharpNamespaces {
    object System {
        data object Root : DotnetAspCSharpNamespace {
            override val value = "System"
        }

        object Threading {
            data object Root : DotnetAspCSharpNamespace {
                override val value = "System.Threading"
            }

            data object Tasks : DotnetAspCSharpNamespace {
                override val value = "System.Threading.Tasks"
            }
        }
    }

    object Microsoft {
        object AspNetCore {
            data object Builder : DotnetAspCSharpNamespace {
                override val value = "Microsoft.AspNetCore.Builder"
            }

            data object Mvc : DotnetAspCSharpNamespace {
                override val value = "Microsoft.AspNetCore.Mvc"
            }
        }

        object Extensions {
            data object DependencyInjection : DotnetAspCSharpNamespace {
                override val value = "Microsoft.Extensions.DependencyInjection"
            }
        }
    }
}
