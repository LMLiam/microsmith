package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpNamespace

internal object DotnetAspCSharpNamespaces {
    object Microsoft {
        object AspNetCore {
            data object Builder : DotnetCSharpNamespace {
                override val value = "Microsoft.AspNetCore.Builder"
            }

            data object Mvc : DotnetCSharpNamespace {
                override val value = "Microsoft.AspNetCore.Mvc"
            }
        }

        object Extensions {
            data object DependencyInjection : DotnetCSharpNamespace {
                override val value = "Microsoft.Extensions.DependencyInjection"
            }
        }
    }
}
