package io.github.lmliam.microsmith.plugin.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.EnumRef
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.MessageRef
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallCallee
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptCallSite
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptLiteral
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolContributor
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolDeclarationSite
import io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolDefinition

internal class ProtobufScriptSymbolContributor : ScriptSymbolContributor {
    override val id: String = "microsmith.protobuf"

    override val declarationCallNames: Set<String> =
        setOf(
            MESSAGE_CALL,
            ENUM_CALL,
        )

    override fun discover(site: ScriptSymbolDeclarationSite): ScriptSymbolDefinition? {
        val declarationCall = site.call.callee as? ScriptCallCallee.Named ?: return null

        val declarationName = (site.call.arguments.firstOrNull() as? ScriptLiteral.StringValue)?.value ?: return null

        val protobufIndex = site.enclosingCalls.indexOfLast(::isProtobufCall)

        if (protobufIndex < 0) return null

        val path = buildList {
            site.enclosingCalls.drop(protobufIndex + 1).forEach {
                addNamespaceSegments(
                    call = it,
                    target = this,
                )
            }

            add(declarationName)
        }

        val valueType =
            when (declarationCall.name) {
                MESSAGE_CALL -> MessageRef::class
                ENUM_CALL -> EnumRef::class
                else -> return null
            }

        return ScriptSymbolDefinition(
            propertyName = declarationName,
            valueType = valueType,
            kind = declarationCall.name,
            valueKey = path.joinToString("."),
            qualifiedPath = path,
        )
    }

    override fun createValue(kind: String, valueKey: String): Any =
        when (kind) {
            MESSAGE_CALL -> MessageRef(valueKey)
            ENUM_CALL -> EnumRef(valueKey)
            else -> error("Unknown protobuf script symbol kind '$kind'")
        }

    private fun isProtobufCall(call: ScriptCallSite): Boolean =
        (call.callee as? ScriptCallCallee.Named)?.name == PROTOBUF_CALL

    private fun addNamespaceSegments(call: ScriptCallSite, target: MutableList<String>) {
        when (val callee = call.callee) {
            is ScriptCallCallee.StringLiteral -> target += callee.value.split('.').filter(String::isNotBlank)

            is ScriptCallCallee.IntLiteral -> target += "v${callee.value}"

            is ScriptCallCallee.Named if callee.name == VERSION_CALL -> {
                val version = (call.arguments.firstOrNull() as? ScriptLiteral.IntValue)?.value

                if (version != null) target += "v$version"
            }

            is ScriptCallCallee.Named,
            ScriptCallCallee.Other -> Unit
        }
    }
}

private const val PROTOBUF_CALL = "protobuf"
private const val VERSION_CALL = "version"
private const val MESSAGE_CALL = "message"
private const val ENUM_CALL = "enum"
