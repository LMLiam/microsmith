package me.liam.microsmith.dsl.schemas.protobuf

class ProtobufBuilder : ProtobufScope {
    private val schemas = mutableSetOf<ProtobufSchema>()

    override fun message(name: String, block: MessageScope.() -> Unit) {
        schemas += ProtobufMessageSchema(name, message = MessageBuilder(name).apply(block).build())
    }

    override fun enum(name: String, block: EnumScope.() -> Unit) {
        schemas += ProtobufEnumSchema(name, enum = EnumBuilder(name).apply(block).build())
    }

    fun build() = schemas.toSet()
}