package io.github.lmliam.microsmith.gen.system

import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.plugin.Environment

@InternalMicrosmithApi
public class SystemEnvironment : Environment {
    override fun variable(name: String): String? = System.getenv(name)

    override fun property(name: String): String? = System.getProperty(name)
}
