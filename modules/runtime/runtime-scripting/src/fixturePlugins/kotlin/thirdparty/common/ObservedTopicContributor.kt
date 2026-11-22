package thirdparty.common

import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolContributor

class ObservedTopicContributor(private val onCreateValue: (ScriptSymbolContributor) -> Unit) : TopicContributor() {
    override fun createValue(kind: String, valueKey: String): Any {
        onCreateValue(this)
        return super.createValue(kind, valueKey)
    }
}
