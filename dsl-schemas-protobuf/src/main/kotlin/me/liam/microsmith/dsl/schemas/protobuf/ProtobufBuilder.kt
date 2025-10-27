package me.liam.microsmith.dsl.schemas.protobuf

class ProtobufBuilder : ProtobufScope {
    private val messages = mutableSetOf<Message>()

    override fun message(name: String, block: MessageScope.() -> Unit) {
        messages += MessageBuilder(name).apply(block).build()
    }

    fun build() = messages.map { ProtobufSchema(it.name, it) }.toSet()
}