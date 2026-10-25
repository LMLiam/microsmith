package io.github.lmliam.microsmith.compile.services.dotnet.csharp

object DotnetCSharpNamespaces {
    object System {
        data object Root : DotnetCSharpNamespace {
            override val value = "System"
        }

        object Threading {
            data object Root : DotnetCSharpNamespace {
                override val value = "System.Threading"
            }

            data object Tasks : DotnetCSharpNamespace {
                override val value = "System.Threading.Tasks"
            }
        }
    }
}
