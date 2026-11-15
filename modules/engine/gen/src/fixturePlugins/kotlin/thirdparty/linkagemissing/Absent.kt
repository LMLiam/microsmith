package thirdparty.linkagemissing

import io.github.lmliam.microsmith.plugin.PluginServices

object Absent {
    fun services(): PluginServices = object : PluginServices {}
}
