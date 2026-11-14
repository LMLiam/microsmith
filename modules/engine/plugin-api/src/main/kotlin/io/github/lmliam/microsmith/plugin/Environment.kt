package io.github.lmliam.microsmith.plugin

interface Environment {
    fun variable(name: String): String?

    fun property(name: String): String?
}
