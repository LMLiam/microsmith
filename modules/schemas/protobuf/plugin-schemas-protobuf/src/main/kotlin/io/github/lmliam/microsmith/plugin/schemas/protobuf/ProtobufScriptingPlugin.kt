package io.github.lmliam.microsmith.plugin.schemas.protobuf

import io.github.lmliam.microsmith.runtime.scripting.api.MicrosmithScriptingPlugin
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolContributor

public class ProtobufScriptingPlugin : MicrosmithScriptingPlugin {
    override val id: String = PROTOBUF_PLUGIN_ID

    override val scriptSymbolContributors: Set<ScriptSymbolContributor> = setOf(ProtobufScriptSymbolContributor())
}
