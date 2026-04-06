package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp

internal object DotnetAspCSharpModifiers {
    val public = listOf(CSharp.Modifier.PUBLIC)
    val publicAbstract = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT)
    val publicAsync = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ASYNC)
    val publicSealed = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED)
    val publicStatic = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.STATIC)
    val protectedAbstract = listOf(CSharp.Modifier.PROTECTED, CSharp.Modifier.ABSTRACT)
    val private = listOf(CSharp.Modifier.PRIVATE)
    val params = listOf(CSharp.Modifier.PARAMS)
}
