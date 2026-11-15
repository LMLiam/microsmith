package io.github.lmliam.microsmith.compile.services.dotnet.csharp

public object DotnetCSharpTypes {
    public object System {
        public data object InvalidOperationException : DotnetCSharpTypeName {
            override val value: String = "InvalidOperationException"
        }
    }

    public object Primitives {
        public data object String : DotnetCSharpTypeName {
            override val value: kotlin.String = "string"
        }

        public data object Int : DotnetCSharpTypeName {
            override val value: kotlin.String = "int"
        }

        public data object Object : DotnetCSharpTypeName {
            override val value: kotlin.String = "object"
        }
    }

    public object Threading {
        public data object CancellationToken : DotnetCSharpTypeName {
            override val value: String = "CancellationToken"
        }

        public data object Task : DotnetCSharpTypeName {
            override val value: String = "Task"
        }
    }
}
