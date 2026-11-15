package io.github.lmliam.microsmith.plugin

public interface Environment {
    public fun variable(name: String): String?

    public fun property(name: String): String?
}
