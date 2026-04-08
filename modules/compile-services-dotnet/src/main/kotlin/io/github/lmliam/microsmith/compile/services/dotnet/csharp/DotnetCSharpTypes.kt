package io.github.lmliam.microsmith.compile.services.dotnet.csharp

object DotnetCSharpTypes {
    object System {
        data object InvalidOperationException : DotnetCSharpTypeName {
            override val value = "InvalidOperationException"
        }
    }

    object Primitives {
        data object String : DotnetCSharpTypeName {
            override val value = "string"
        }

        data object Int : DotnetCSharpTypeName {
            override val value = "int"
        }

        data object Object : DotnetCSharpTypeName {
            override val value = "object"
        }
    }

    object Threading {
        data object CancellationToken : DotnetCSharpTypeName {
            override val value = "CancellationToken"
        }

        data object Task : DotnetCSharpTypeName {
            override val value = "Task"
        }
    }
}
