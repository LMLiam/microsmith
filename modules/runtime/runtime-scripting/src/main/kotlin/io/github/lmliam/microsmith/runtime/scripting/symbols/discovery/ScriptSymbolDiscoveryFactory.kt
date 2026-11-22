package io.github.lmliam.microsmith.runtime.scripting.symbols.discovery

import io.github.lmliam.microsmith.runtime.scripting.symbols.parsing.KotlinScriptCallParser
import io.github.lmliam.microsmith.runtime.scripting.symbols.parsing.KotlinScriptCallSiteParser
import io.github.lmliam.microsmith.runtime.scripting.symbols.parsing.KotlinScriptLiteralParser
import io.github.lmliam.microsmith.runtime.scripting.symbols.parsing.KotlinScriptTokenizer

internal fun createScriptSymbolDiscovery(): ScriptSymbolDiscovery =
    ScriptSymbolDiscovery(
        KotlinScriptCallSiteParser(KotlinScriptTokenizer(), KotlinScriptCallParser(KotlinScriptLiteralParser())),
    )
