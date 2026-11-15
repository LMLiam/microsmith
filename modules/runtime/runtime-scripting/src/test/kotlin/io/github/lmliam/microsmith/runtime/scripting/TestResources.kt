package io.github.lmliam.microsmith.runtime.scripting

internal object TestResources {
    fun read(resource: String): String =
        checkNotNull(TestResources::class.java.getResource(resource)) { "Missing test resource $resource" }.readText()
}
