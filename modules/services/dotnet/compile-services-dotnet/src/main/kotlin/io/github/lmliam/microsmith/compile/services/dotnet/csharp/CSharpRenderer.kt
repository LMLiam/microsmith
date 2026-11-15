package io.github.lmliam.microsmith.compile.services.dotnet.csharp

public class CSharpRenderer {
    public fun render(file: CSharp.File): String = renderCSharp(file)
}
