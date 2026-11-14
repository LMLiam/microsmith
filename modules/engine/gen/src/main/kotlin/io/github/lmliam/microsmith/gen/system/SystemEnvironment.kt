package io.github.lmliam.microsmith.gen.system

import io.github.lmliam.microsmith.plugin.Environment

class SystemEnvironment : Environment {
    override fun variable(name: String): String? = System.getenv(name)

    override fun property(name: String): String? = System.getProperty(name)
}
