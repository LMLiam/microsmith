val redundantCast = "text" as String

microsmith {
    services {
        dotnet {
            target(NET10)
            solutions { "Platform" {} }
        }
    }
}
