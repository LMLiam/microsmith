package io.github.lmliam.microsmith.gen.system

import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.plugin.Console

@InternalMicrosmithApi
public class SystemConsole : Console {
    override fun out(line: String) {
        System.out.println(line)
    }

    override fun err(line: String) {
        System.err.println(line)
    }
}
