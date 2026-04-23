package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharpFileBuilder
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpNamespace

internal object DotnetAspCSharpNamespaces {
    data object System : DotnetCSharpNamespace {
        override val value = "System"
    }

    object SystemThreading {
        data object Root : DotnetCSharpNamespace {
            override val value = "System.Threading"
        }

        data object Tasks : DotnetCSharpNamespace {
            override val value = "System.Threading.Tasks"
        }
    }

    object Microsoft {
        object AspNetCore {
            data object Builder : DotnetCSharpNamespace {
                override val value = "Microsoft.AspNetCore.Builder"
            }

            data object Mvc : DotnetCSharpNamespace {
                override val value = "Microsoft.AspNetCore.Mvc"
            }

            data object ModelBinding : DotnetCSharpNamespace {
                override val value = "Microsoft.AspNetCore.Mvc.ModelBinding"
            }
        }

        object Extensions {
            data object DependencyInjection : DotnetCSharpNamespace {
                override val value = "Microsoft.Extensions.DependencyInjection"
            }
        }
    }
}

internal fun CSharpFileBuilder.using(namespace: DotnetCSharpNamespace) {
    using(namespace.value)
}
