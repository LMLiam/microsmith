package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import java.util.Locale

internal object DotnetAspControllerFileRenderer {
    fun renderControllerBaseFile(artifact: DotnetAspServiceArtifact): String = buildString {
        appendLine("using System;")
        appendLine("using System.Threading;")
        appendLine("using System.Threading.Tasks;")
        appendLine("using ${contractsNamespace(artifact)};")
        appendLine("using Microsoft.AspNetCore.Mvc;")
        appendLine()
        appendLine("namespace ${controllersNamespace(artifact)};")
        appendLine()
        appendLine("[ApiController]")
        appendLine(
            "public abstract class ${controllerBaseTypeName(artifact)} : " +
                MICROSMITH_CONTROLLER_BASE_TYPE_NAME,
        )
        appendLine("{")
        artifact.endpoints.forEach { endpoint ->
            append(renderActionMethod(endpoint))
            appendLine()
            append(renderAbstractHandler(endpoint))
            appendLine()
            append(renderResultMapper(endpoint))
            appendLine()
        }
        appendLine("}")
    }

    private fun renderActionMethod(endpoint: DotnetAspEndpointArtifact): String = buildString {
        val routeLiteral = escapeDotnetAspCsharpStringLiteral(endpoint.route)
        val operationNameLiteral = escapeDotnetAspCsharpStringLiteral(endpoint.operationName)
        appendLine(
            "    [${httpAttributeName(endpoint.method)}($routeLiteral, Name = $operationNameLiteral)]",
        )
        endpoint.responses.forEach { response ->
            appendLine(
                "    [ProducesResponseType(typeof(${responseAttributeType(response)}), ${response.statusCode})]",
            )
        }
        append("    public async Task<ActionResult<${resultBaseTypeName(endpoint)}>> ${endpoint.operationName}(")
        append(actionParameters(endpoint).joinToString(", "))
        appendLine(")")
        appendLine("    {")
        endpoint.bindings.headers?.let { binding ->
            append(renderHeadersInitializer(binding))
            appendLine()
        }
        appendLine(
            "        var result = await On${endpoint.operationName}Async(" +
                handlerArguments(endpoint).joinToString(", ") +
                ");",
        )
        appendLine("        return Map${endpoint.operationName}Result(result);")
        appendLine("    }")
    }

    private fun renderAbstractHandler(endpoint: DotnetAspEndpointArtifact): String = buildString {
        append("    protected abstract Task<${resultBaseTypeName(endpoint)}> On${endpoint.operationName}Async(")
        append(handlerParameters(endpoint).joinToString(", "))
        appendLine(");")
    }

    private fun renderResultMapper(endpoint: DotnetAspEndpointArtifact): String = buildString {
        appendLine(
            "    private ActionResult<${resultBaseTypeName(endpoint)}> " +
                "Map${endpoint.operationName}Result(${resultBaseTypeName(endpoint)} result)",
        )
        appendLine("    {")
        appendLine("        return result switch")
        appendLine("        {")
        endpoint.responses.forEach { response ->
            append("            ${resultVariantTypeName(endpoint, response)} response => Respond(")
            append(renderResponseBodyArgument(response))
            append(", ${response.statusCode}")
            response.headers.forEach { header ->
                append(
                    ", (${escapeDotnetAspCsharpStringLiteral(header.name)}, " +
                        "response.${dotnetAspHeaderPropertyName(header.name)})",
                )
            }
            appendLine("),")
        }
        val unsupportedMessage =
            escapeDotnetAspCsharpStringLiteral("Unsupported ${endpoint.operationName} result type.")
        appendLine("            _ => throw new InvalidOperationException(")
        appendLine("                $unsupportedMessage + result.GetType().FullName + \".\"),")
        appendLine("        };")
        appendLine("    }")
    }

    private fun renderHeadersInitializer(binding: DotnetAspHeadersBindingArtifact): String = buildString {
        appendLine("        var headers = new ${binding.typeName}")
        appendLine("        {")
        binding.headers.forEachIndexed { index, header ->
            val suffix = if (index == binding.headers.lastIndex) "" else ","
            val headerPropertyName = dotnetAspPascalIdentifier(header.name)
            val headerLiteral = escapeDotnetAspCsharpStringLiteral(header.headerName)
            appendLine("            $headerPropertyName = ReadHeader($headerLiteral)$suffix")
        }
        appendLine("        };")
    }

    private fun actionParameters(endpoint: DotnetAspEndpointArtifact): List<String> = buildList {
        endpoint.bindings.path?.let { add("[FromRoute] ${it.typeName} path") }
        endpoint.bindings.query?.let { add("[FromQuery] ${it.typeName} query") }
        endpoint.bindings.body?.let { add("[FromBody] ${it.typeName} body") }
        add("CancellationToken cancellationToken")
    }

    private fun handlerParameters(endpoint: DotnetAspEndpointArtifact): List<String> = buildList {
        endpoint.bindings.path?.let { add("${it.typeName} path") }
        endpoint.bindings.query?.let { add("${it.typeName} query") }
        endpoint.bindings.headers?.let { add("${it.typeName} headers") }
        endpoint.bindings.body?.let { add("${it.typeName} body") }
        add("CancellationToken cancellationToken")
    }

    private fun handlerArguments(endpoint: DotnetAspEndpointArtifact): List<String> = buildList {
        endpoint.bindings.path?.let { add("path") }
        endpoint.bindings.query?.let { add("query") }
        endpoint.bindings.headers?.let { add("headers") }
        endpoint.bindings.body?.let { add("body") }
        add("cancellationToken")
    }

    private fun responseAttributeType(response: DotnetAspResponseArtifact): String =
        if (response.statusCode == HTTP_NO_CONTENT_STATUS_CODE) "void" else response.model.typeName

    private fun renderResponseBodyArgument(response: DotnetAspResponseArtifact): String =
        if (response.statusCode == HTTP_NO_CONTENT_STATUS_CODE) "null" else "response.$RESULT_BODY_PROPERTY_NAME"

    private fun httpAttributeName(method: String): String =
        "Http" + method.lowercase(Locale.ROOT).replaceFirstChar(Char::uppercase)
}
