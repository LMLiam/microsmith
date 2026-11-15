package io.github.lmliam.microsmith.runtime.scripting.api

public interface MicrosmithScriptingPlugin {
    public val id: String

    public val scriptSymbolContributors: Set<ScriptSymbolContributor>
}
