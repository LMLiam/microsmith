package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspHeaderFieldArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelLocality
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestFieldArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import java.util.Locale

internal object DotnetAspProjectRenderer {
    fun renderProgramFile(artifact: DotnetAspServiceArtifact): String = """
        using ${hostingNamespace(artifact)};

        var builder = WebApplication.CreateBuilder(args);
        builder.AddMicrosmith();

        var app = builder.Build();
        app.MapMicrosmith();
        app.Run();

        public partial class Program { }
    """.trimIndent()

    fun renderHostingExtensionsFile(artifact: DotnetAspServiceArtifact): String = """
        namespace ${hostingNamespace(artifact)};

        using Microsoft.AspNetCore.Builder;
        using Microsoft.Extensions.DependencyInjection;

        public static class MicrosmithHostingExtensions
        {
            public static WebApplicationBuilder AddMicrosmith(this WebApplicationBuilder builder)
            {
                builder.Services.AddControllers();
                return builder;
            }

            public static WebApplication MapMicrosmith(this WebApplication app)
            {
                app.MapControllers();
                return app;
            }
        }
    """.trimIndent()

    fun renderMicrosmithControllerBaseFile(artifact: DotnetAspServiceArtifact): String = """
        namespace ${controllersNamespace(artifact)};

        using Microsoft.AspNetCore.Mvc;

        public abstract class $MICROSMITH_CONTROLLER_BASE_TYPE_NAME : ControllerBase
        {
            protected ActionResult Respond(object? body, int statusCode, params (string Name, string? Value)[] headers)
            {
                foreach (var (name, value) in headers)
                {
                    if (value is not null)
                    {
                        Response.Headers[name] = value;
                    }
                }

                if (statusCode == 204)
                {
                    return StatusCode(statusCode);
                }

                return new ObjectResult(body)
                {
                    StatusCode = statusCode
                };
            }

            protected string? ReadHeader(string headerName)
            {
                return Request.Headers.TryGetValue(headerName, out var values)
                    ? values.ToString()
                    : null;
            }
        }
    """.trimIndent()

    fun renderControllerBaseFile(artifact: DotnetAspServiceArtifact): String = buildString {
        appendLine("namespace ${controllersNamespace(artifact)};")
        appendLine()
        appendLine("using System;")
        appendLine("using System.Threading;")
        appendLine("using System.Threading.Tasks;")
        appendLine("using ${contractsNamespace(artifact)};")
        appendLine("using Microsoft.AspNetCore.Mvc;")
        appendLine()
        appendLine("[ApiController]")
        appendLine("public abstract class ${controllerBaseTypeName(artifact)} : $MICROSMITH_CONTROLLER_BASE_TYPE_NAME")
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

    fun renderServiceModelsFile(artifact: DotnetAspServiceArtifact): String = buildString {
        appendLine("namespace ${contractsNamespace(artifact)};")
        appendLine()
        artifact.contractModels
            .distinctBy(DotnetAspModelArtifact::typeName)
            .filter { it.locality == DotnetAspModelLocality.SHARED }
            .sortedBy(DotnetAspModelArtifact::typeName)
            .forEachIndexed { index, model ->
                if (index > 0) {
                    appendLine()
                }
                append(renderRecordType(model.typeName, model.model.fields))
            }
    }

    fun renderRequestModelsFile(artifact: DotnetAspServiceArtifact): String = buildString {
        appendLine("namespace ${contractsNamespace(artifact)};")
        appendLine()
        appendLine("using Microsoft.AspNetCore.Mvc.ModelBinding;")
        val elements = buildList {
            collectRequestBindings(artifact).forEach { add(renderRequestBindingType(it)) }
            collectHeaderBindings(artifact).forEach { add(renderHeadersBindingType(it)) }
            artifact.endpoints.forEach { endpoint ->
                endpoint.bindings.body
                    ?.takeIf { it.locality == DotnetAspModelLocality.INLINE }
                    ?.let { add(renderRecordType(it.typeName, it.model.fields)) }
            }
        }.distinct()
        elements.forEachIndexed { index, typeBlock ->
            if (index > 0) {
                appendLine()
            }
            append(typeBlock)
        }
    }

    fun renderResponseModelsFile(artifact: DotnetAspServiceArtifact): String = buildString {
        appendLine("namespace ${contractsNamespace(artifact)};")
        appendLine()
        val elements = buildList {
            artifact.endpoints.forEach { endpoint ->
                endpoint.responses
                    .map(DotnetAspResponseArtifact::model)
                    .filter { it.locality == DotnetAspModelLocality.INLINE }
                    .distinctBy(DotnetAspModelArtifact::typeName)
                    .forEach { model -> add(renderRecordType(model.typeName, model.model.fields)) }
            }
            artifact.endpoints.forEach { endpoint ->
                add(renderResultBaseType(endpoint))
                endpoint.responses.forEach { response ->
                    add(renderResultVariantType(endpoint, response))
                }
            }
        }.distinct()
        elements.forEachIndexed { index, typeBlock ->
            if (index > 0) {
                appendLine()
            }
            append(typeBlock)
        }
    }

    private fun renderActionMethod(endpoint: DotnetAspEndpointArtifact): String = buildString {
        appendLine("    [${httpAttributeName(endpoint.method)}(${escapeCsharpStringLiteral(endpoint.route)}, Name = ${escapeCsharpStringLiteral(endpoint.operationName)})]")
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
        appendLine("        var result = await On${endpoint.operationName}Async(${handlerArguments(endpoint).joinToString(", ")});")
        appendLine("        return Map${endpoint.operationName}Result(result);")
        appendLine("    }")
    }

    private fun renderAbstractHandler(endpoint: DotnetAspEndpointArtifact): String = buildString {
        append("    protected abstract Task<${resultBaseTypeName(endpoint)}> On${endpoint.operationName}Async(")
        append(handlerParameters(endpoint).joinToString(", "))
        appendLine(");")
    }

    private fun renderResultMapper(endpoint: DotnetAspEndpointArtifact): String = buildString {
        appendLine("    private ActionResult<${resultBaseTypeName(endpoint)}> Map${endpoint.operationName}Result(${resultBaseTypeName(endpoint)} result)")
        appendLine("    {")
        appendLine("        return result switch")
        appendLine("        {")
        endpoint.responses.forEach { response ->
            append("            ${resultVariantTypeName(endpoint, response)} response => Respond(")
            append(
                if (response.statusCode == 204) {
                    "null"
                } else {
                    "response.$RESULT_BODY_PROPERTY_NAME"
                },
            )
            append(", ${response.statusCode}")
            response.headers.forEach { header ->
                append(", (${escapeCsharpStringLiteral(header.name)}, response.${dotnetAspHeaderPropertyName(header.name)})")
            }
            appendLine("),")
        }
        appendLine(
            "            _ => throw new InvalidOperationException(" +
                "${escapeCsharpStringLiteral("Unsupported ${endpoint.operationName} result type.")} + " +
                "result.GetType().FullName + \".\"),",
        )
        appendLine("        };")
        appendLine("    }")
    }

    private fun renderHeadersInitializer(binding: DotnetAspHeadersBindingArtifact): String = buildString {
        appendLine("        var headers = new ${binding.typeName}")
        appendLine("        {")
        binding.headers.forEachIndexed { index, header ->
            val suffix = if (index == binding.headers.lastIndex) "" else ","
            appendLine(
                "            ${dotnetAspPascalIdentifier(header.name)} = " +
                    "ReadHeader(${escapeCsharpStringLiteral(header.headerName)})$suffix",
            )
        }
        appendLine("        };")
    }

    private fun renderRecordType(typeName: String, fields: List<DotnetField>): String = buildString {
        appendLine("public sealed record $typeName")
        appendLine("{")
        fields.forEach { field ->
            appendLine(
                "    public ${renderModelPropertyType(field.type)} ${dotnetAspPascalIdentifier(field.name)} { get; set; }" +
                    renderInitializer(field.type),
            )
        }
        appendLine("}")
    }

    private fun renderRequestBindingType(binding: DotnetAspRequestBindingArtifact): String = buildString {
        appendLine("public sealed record ${binding.typeName}")
        appendLine("{")
        binding.fields.forEach { field ->
            if (!field.optional && field.defaultValue == null) {
                appendLine("    [BindRequired]")
            }
            appendLine(
                "    public ${renderBindingPropertyType(field)} ${dotnetAspPascalIdentifier(field.name)} { get; set; }" +
                    renderBindingInitializer(field),
            )
        }
        appendLine("}")
    }

    private fun renderHeadersBindingType(binding: DotnetAspHeadersBindingArtifact): String = buildString {
        appendLine("public sealed record ${binding.typeName}")
        appendLine("{")
        binding.headers.forEach { header ->
            appendLine("    public string? ${dotnetAspPascalIdentifier(header.name)} { get; set; } = null;")
        }
        appendLine("}")
    }

    private fun renderResultBaseType(endpoint: DotnetAspEndpointArtifact): String =
        "public abstract record ${resultBaseTypeName(endpoint)};"

    private fun renderResultVariantType(
        endpoint: DotnetAspEndpointArtifact,
        response: DotnetAspResponseArtifact,
    ): String = buildString {
        append("public sealed record ${resultVariantTypeName(endpoint, response)}(")
        val parameters = buildList {
            if (response.statusCode != 204) {
                add("${response.model.typeName} $RESULT_BODY_PROPERTY_NAME")
            }
            response.headers.forEach { header ->
                add("string? ${dotnetAspHeaderPropertyName(header.name)} = null")
            }
        }
        append(parameters.joinToString(", "))
        append(") : ${resultBaseTypeName(endpoint)};")
    }

    private fun actionParameters(endpoint: DotnetAspEndpointArtifact): List<String> = buildList {
        endpoint.bindings.path?.let {
            add("[FromRoute] ${it.typeName} path")
        }
        endpoint.bindings.query?.let {
            add("[FromQuery] ${it.typeName} query")
        }
        endpoint.bindings.body?.let {
            add("[FromBody] ${it.typeName} body")
        }
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
        if (response.statusCode == 204) {
            "void"
        } else {
            response.model.typeName
        }

    private fun httpAttributeName(method: String): String =
        "Http" + method.lowercase(Locale.ROOT).replaceFirstChar(Char::uppercase)

    private fun renderModelPropertyType(type: DotnetFieldType): String = when (type) {
        is DotnetFieldType.Reference -> type.target
        else -> type.csharpType
    }

    private fun renderBindingPropertyType(field: DotnetAspRequestFieldArtifact): String {
        val baseType = renderModelPropertyType(field.type)
        return if (field.optional && field.defaultValue == null) "$baseType?" else baseType
    }

    private fun renderInitializer(type: DotnetFieldType): String = when (type) {
        DotnetFieldType.String -> " = string.Empty;"
        DotnetFieldType.Char -> " = 'A';"
        DotnetFieldType.Byte,
        DotnetFieldType.SignedByte,
        DotnetFieldType.Short,
        DotnetFieldType.UnsignedShort,
        DotnetFieldType.Int,
        DotnetFieldType.UnsignedInt,
        DotnetFieldType.Long,
        DotnetFieldType.UnsignedLong,
        DotnetFieldType.NativeInt,
        DotnetFieldType.UnsignedNativeInt,
        -> " = 0;"
        DotnetFieldType.Float -> " = 0F;"
        DotnetFieldType.Double -> " = 0D;"
        DotnetFieldType.Decimal -> " = 0M;"
        DotnetFieldType.Bool -> " = false;"
        DotnetFieldType.Guid -> " = Guid.Empty;"
        DotnetFieldType.DateOnly -> " = DateOnly.MinValue;"
        DotnetFieldType.TimeOnly -> " = TimeOnly.MinValue;"
        DotnetFieldType.DateTime -> " = DateTime.UnixEpoch;"
        DotnetFieldType.DateTimeOffset -> " = DateTimeOffset.UnixEpoch;"
        DotnetFieldType.TimeSpan -> " = TimeSpan.Zero;"
        is DotnetFieldType.Reference -> " = null!;"
    }

    private fun renderBindingInitializer(field: DotnetAspRequestFieldArtifact): String {
        val defaultValue = field.defaultValue
        return when {
            defaultValue != null -> " = ${renderDefaultExpression(field.type, defaultValue)};"
            field.optional -> " = null;"
            else -> renderInitializer(field.type)
        }
    }

    private fun renderDefaultExpression(type: DotnetFieldType, defaultValue: Any): String = when (type) {
        DotnetFieldType.String -> escapeCsharpStringLiteral(defaultValue.toString())
        DotnetFieldType.Char -> escapeCsharpCharLiteral(defaultValue.toString().first())
        DotnetFieldType.Byte,
        DotnetFieldType.SignedByte,
        DotnetFieldType.Short,
        DotnetFieldType.UnsignedShort,
        DotnetFieldType.Int,
        -> defaultValue.toString()
        DotnetFieldType.UnsignedInt -> "${defaultValue}U"
        DotnetFieldType.Long -> "${defaultValue}L"
        DotnetFieldType.UnsignedLong -> "${defaultValue}UL"
        DotnetFieldType.NativeInt -> "(nint)$defaultValue"
        DotnetFieldType.UnsignedNativeInt -> "(nuint)${defaultValue}UL"
        DotnetFieldType.Float -> "${defaultValue.toString().ensureDecimal()}F"
        DotnetFieldType.Double -> "${defaultValue.toString().ensureDecimal()}D"
        DotnetFieldType.Decimal -> "${defaultValue.toString().ensureDecimal()}M"
        DotnetFieldType.Bool -> defaultValue.toString().lowercase(Locale.ROOT)
        DotnetFieldType.Guid -> "Guid.Parse(${escapeCsharpStringLiteral(defaultValue.toString())})"
        DotnetFieldType.DateOnly ->
            "DateOnly.Parse(${escapeCsharpStringLiteral(defaultValue.toString())}, global::System.Globalization.CultureInfo.InvariantCulture)"
        DotnetFieldType.TimeOnly ->
            "TimeOnly.Parse(${escapeCsharpStringLiteral(defaultValue.toString())}, global::System.Globalization.CultureInfo.InvariantCulture)"
        DotnetFieldType.DateTime ->
            "DateTime.Parse(${escapeCsharpStringLiteral(defaultValue.toString())}, global::System.Globalization.CultureInfo.InvariantCulture, global::System.Globalization.DateTimeStyles.RoundtripKind)"
        DotnetFieldType.DateTimeOffset ->
            "DateTimeOffset.Parse(${escapeCsharpStringLiteral(defaultValue.toString())}, global::System.Globalization.CultureInfo.InvariantCulture, global::System.Globalization.DateTimeStyles.RoundtripKind)"
        DotnetFieldType.TimeSpan ->
            "TimeSpan.Parse(${escapeCsharpStringLiteral(defaultValue.toString())}, global::System.Globalization.CultureInfo.InvariantCulture)"
        is DotnetFieldType.Reference ->
            "global::System.Text.Json.JsonSerializer.Deserialize<${type.target}>(" +
                "${escapeCsharpStringLiteral(defaultValue.toString())})!"
    }

    private fun escapeCsharpStringLiteral(value: String): String = buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (char.code < 0x20) {
                        append("\\u%04x".format(char.code))
                    } else {
                        append(char)
                    }
                }
            }
        }
        append('"')
    }

    private fun escapeCsharpCharLiteral(value: Char): String = when (value) {
        '\\' -> "'\\\\'"
        '\'' -> "'\\''"
        '\n' -> "'\\n'"
        '\r' -> "'\\r'"
        '\t' -> "'\\t'"
        '\b' -> "'\\b'"
        '\u000C' -> "'\\f'"
        else -> if (value.code < 0x20) "'\\u%04x'".format(value.code) else "'$value'"
    }

    private fun String.ensureDecimal(): String =
        if (contains('.') || contains('E', ignoreCase = true)) this else "$this.0"
}
