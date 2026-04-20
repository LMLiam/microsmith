package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspEndpointArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspHeadersBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspModelArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestBindingArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestFieldArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspResponseArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import java.util.Locale

internal object DotnetAspProjectRenderer {
    fun renderProgramFile(): String = """
        var builder = WebApplication.CreateBuilder(args);

        builder.Services.AddControllers();

        var app = builder.Build();

        app.Use(async (context, next) =>
        {
            try
            {
                await next();
            }
            catch (BadHttpRequestException exception)
            {
                await Results.Json(
                    new
                    {
                        detail = exception.Message,
                    },
                    statusCode: StatusCodes.Status400BadRequest).ExecuteAsync(context);
            }
        });

        app.MapControllers();

        app.Run();

        public partial class Program { }
    """.trimIndent()

    fun renderControllerFile(artifact: DotnetAspServiceArtifact): String = buildString {
        val projectNamespace = artifact.id.projectName
        appendLine("namespace $projectNamespace.Controllers;")
        appendLine()
        appendLine("using $projectNamespace.Bindings;")
        appendLine("using $projectNamespace.Generated;")
        appendLine("using $projectNamespace.Models;")
        appendLine("using System.Globalization;")
        appendLine("using Microsoft.AspNetCore.Mvc;")
        appendLine()
        appendLine("[ApiController]")
        appendLine("public sealed class ${artifact.serviceName}Controller : ControllerBase")
        appendLine("{")
        artifact.endpoints.forEach { endpoint ->
            append(renderEndpoint(endpoint))
            appendLine()
        }
        append(renderRequestedStatusHelper())
        appendLine("}")
    }

    fun renderModelFile(projectNamespace: String, model: DotnetAspModelArtifact): String = buildString {
        appendLine("namespace $projectNamespace.Models;")
        appendLine()
        appendLine("public sealed class ${model.typeName}")
        appendLine("{")
        model.model.fields.forEach { field ->
            appendLine("    public ${renderModelPropertyType(field.type)} ${field.name} { get; set; }${renderInitializer(field.type)}")
        }
        appendLine("}")
    }

    fun renderRequestBindingFile(projectNamespace: String, binding: DotnetAspRequestBindingArtifact): String = buildString {
        val referenceTargets = binding.fields.any { it.type is DotnetFieldType.Reference }
        appendLine("namespace $projectNamespace.Bindings;")
        appendLine()
        if (referenceTargets) {
            appendLine("using $projectNamespace.Models;")
            appendLine()
        }
        appendLine("public sealed class ${binding.typeName}")
        appendLine("{")
        binding.fields.forEach { field ->
            appendLine(
                "    public ${renderBindingPropertyType(field)} ${field.name} { get; set; }${renderBindingInitializer(field)}",
            )
        }
        appendLine("}")
    }

    fun renderHeadersBindingFile(projectNamespace: String, binding: DotnetAspHeadersBindingArtifact): String = buildString {
        appendLine("namespace $projectNamespace.Bindings;")
        appendLine()
        appendLine("public sealed class ${binding.typeName}")
        appendLine("{")
        binding.headers.forEach { header ->
            appendLine("    public string? ${header.name} { get; set; } = null;")
        }
        appendLine("}")
    }

    fun renderRequestParserFile(projectNamespace: String): String = """
        namespace $projectNamespace.Generated;

        using System;
        using System.Globalization;
        using System.Text.Json;
        using Microsoft.AspNetCore.Http;
        using Microsoft.AspNetCore.Routing;
        using Microsoft.Extensions.Primitives;

        internal static class MicrosmithRequestParser
        {
            private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

            internal static string? ReadRouteValue(RouteValueDictionary routeValues, string name) =>
                routeValues.TryGetValue(name, out var value) ? value?.ToString() : null;

            internal static string? ReadQueryValue(IQueryCollection query, string name) =>
                ReadStringValues(query[name]);

            internal static string? ReadHeaderValue(IHeaderDictionary headers, string name) =>
                ReadStringValues(headers[name]);

            internal static string RequireString(string? raw, string bindingName) =>
                !string.IsNullOrWhiteSpace(raw) ? raw : throw Missing(bindingName);

            internal static string? OptionalString(string? raw) =>
                string.IsNullOrWhiteSpace(raw) ? null : raw;

            internal static char RequireChar(string? raw, string bindingName)
            {
                var value = RequireString(raw, bindingName);
                if (value.Length != 1)
                {
                    throw Invalid(bindingName, value);
                }

                return value[0];
            }

            internal static char? OptionalChar(string? raw, string bindingName) =>
                string.IsNullOrWhiteSpace(raw) ? null : RequireChar(raw, bindingName);

            internal static byte RequireByte(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => byte.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static byte? OptionalByte(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => byte.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static sbyte RequireSignedByte(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => sbyte.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static sbyte? OptionalSignedByte(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => sbyte.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static short RequireShort(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => short.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static short? OptionalShort(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => short.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static ushort RequireUnsignedShort(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => ushort.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static ushort? OptionalUnsignedShort(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => ushort.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static int RequireInt(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => int.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static int? OptionalInt(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => int.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static uint RequireUnsignedInt(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => uint.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static uint? OptionalUnsignedInt(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => uint.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static long RequireLong(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => long.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static long? OptionalLong(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => long.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static ulong RequireUnsignedLong(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => ulong.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static ulong? OptionalUnsignedLong(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => ulong.Parse(value, NumberStyles.Integer, CultureInfo.InvariantCulture));

            internal static nint RequireNativeInt(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => nint.Parse(value, CultureInfo.InvariantCulture));

            internal static nint? OptionalNativeInt(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => nint.Parse(value, CultureInfo.InvariantCulture));

            internal static nuint RequireUnsignedNativeInt(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => nuint.Parse(value, CultureInfo.InvariantCulture));

            internal static nuint? OptionalUnsignedNativeInt(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => nuint.Parse(value, CultureInfo.InvariantCulture));

            internal static float RequireFloat(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => float.Parse(value, NumberStyles.Float | NumberStyles.AllowThousands, CultureInfo.InvariantCulture));

            internal static float? OptionalFloat(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => float.Parse(value, NumberStyles.Float | NumberStyles.AllowThousands, CultureInfo.InvariantCulture));

            internal static double RequireDouble(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => double.Parse(value, NumberStyles.Float | NumberStyles.AllowThousands, CultureInfo.InvariantCulture));

            internal static double? OptionalDouble(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => double.Parse(value, NumberStyles.Float | NumberStyles.AllowThousands, CultureInfo.InvariantCulture));

            internal static decimal RequireDecimal(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => decimal.Parse(value, NumberStyles.Number, CultureInfo.InvariantCulture));

            internal static decimal? OptionalDecimal(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => decimal.Parse(value, NumberStyles.Number, CultureInfo.InvariantCulture));

            internal static bool RequireBool(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => bool.Parse(value));

            internal static bool? OptionalBool(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => bool.Parse(value));

            internal static Guid RequireGuid(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => Guid.Parse(value));

            internal static Guid? OptionalGuid(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => Guid.Parse(value));

            internal static DateOnly RequireDateOnly(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => DateOnly.Parse(value, CultureInfo.InvariantCulture));

            internal static DateOnly? OptionalDateOnly(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => DateOnly.Parse(value, CultureInfo.InvariantCulture));

            internal static TimeOnly RequireTimeOnly(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => TimeOnly.Parse(value, CultureInfo.InvariantCulture));

            internal static TimeOnly? OptionalTimeOnly(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => TimeOnly.Parse(value, CultureInfo.InvariantCulture));

            internal static DateTime RequireDateTime(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => DateTime.Parse(value, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind));

            internal static DateTime? OptionalDateTime(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => DateTime.Parse(value, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind));

            internal static DateTimeOffset RequireDateTimeOffset(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => DateTimeOffset.Parse(value, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind));

            internal static DateTimeOffset? OptionalDateTimeOffset(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => DateTimeOffset.Parse(value, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind));

            internal static TimeSpan RequireTimeSpan(string? raw, string bindingName) =>
                ParseRequired(raw, bindingName, value => TimeSpan.Parse(value, CultureInfo.InvariantCulture));

            internal static TimeSpan? OptionalTimeSpan(string? raw, string bindingName) =>
                ParseOptionalStruct(raw, bindingName, value => TimeSpan.Parse(value, CultureInfo.InvariantCulture));

            internal static T RequireJson<T>(string? raw, string bindingName) where T : class
            {
                var value = JsonSerializer.Deserialize<T>(RequireString(raw, bindingName), JsonOptions);
                return value ?? throw Invalid(bindingName, raw ?? string.Empty);
            }

            internal static T? OptionalJson<T>(string? raw, string bindingName) where T : class =>
                string.IsNullOrWhiteSpace(raw) ? null : RequireJson<T>(raw, bindingName);

            internal static Guid ParseGuidLiteral(string raw) => Guid.Parse(raw);

            internal static DateOnly ParseDateOnlyLiteral(string raw) =>
                DateOnly.Parse(raw, CultureInfo.InvariantCulture);

            internal static TimeOnly ParseTimeOnlyLiteral(string raw) =>
                TimeOnly.Parse(raw, CultureInfo.InvariantCulture);

            internal static DateTime ParseDateTimeLiteral(string raw) =>
                DateTime.Parse(raw, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind);

            internal static DateTimeOffset ParseDateTimeOffsetLiteral(string raw) =>
                DateTimeOffset.Parse(raw, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind);

            internal static TimeSpan ParseTimeSpanLiteral(string raw) =>
                TimeSpan.Parse(raw, CultureInfo.InvariantCulture);

            internal static T ParseJsonLiteral<T>(string raw) where T : class =>
                JsonSerializer.Deserialize<T>(raw, JsonOptions) ?? throw new InvalidOperationException($"Invalid JSON literal for {typeof(T).Name}.");

            private static T ParseRequired<T>(string? raw, string bindingName, Func<string, T> parser)
            {
                try
                {
                    return parser(RequireString(raw, bindingName));
                }
                catch (Exception) when (!string.IsNullOrWhiteSpace(raw))
                {
                    throw Invalid(bindingName, raw ?? string.Empty);
                }
            }

            private static T? ParseOptionalStruct<T>(string? raw, string bindingName, Func<string, T> parser)
                where T : struct
            {
                if (string.IsNullOrWhiteSpace(raw))
                {
                    return null;
                }

                return ParseRequired(raw, bindingName, parser);
            }

            private static string? ReadStringValues(StringValues values) =>
                StringValues.IsNullOrEmpty(values) ? null : values[0];

            private static BadHttpRequestException Missing(string bindingName) =>
                new($"Missing required value for '{bindingName}'.");

            private static BadHttpRequestException Invalid(string bindingName, string raw) =>
                new($"Invalid value '{raw}' for '{bindingName}'.");
        }
    """.trimIndent()

    private fun renderEndpoint(endpoint: DotnetAspEndpointArtifact): String = buildString {
        appendLine("    [Http${endpoint.method.lowercase().replaceFirstChar(Char::uppercase)}(${escapeCsharpStringLiteral(endpoint.route)}, Name = ${escapeCsharpStringLiteral(endpoint.operationName)})]")
        endpoint.responses.forEach { response ->
            appendLine("    [ProducesResponseType(typeof(${response.model.typeName}), ${response.statusCode})]")
        }
        append("    public IActionResult ${endpoint.operationName}(")
        val parameters = buildList {
            endpoint.bindings.body?.let { bodyModel ->
                add("[FromBody] ${bodyModel.typeName} body")
            }
        }
        append(parameters.joinToString(", "))
        appendLine(")")
        appendLine("    {")
        endpoint.bindings.path?.let { binding ->
            append(renderRequestBindingAssignment(binding, RequestSource.PATH))
        }
        endpoint.bindings.query?.let { binding ->
            append(renderRequestBindingAssignment(binding, RequestSource.QUERY))
        }
        endpoint.bindings.headers?.let { binding ->
            append(renderHeadersBindingAssignment(binding))
        }
        endpoint.bindings.body?.let {
            appendLine("        _ = body;")
        }
        if (endpoint.responses.size > 1) {
            appendLine("        var requestedStatusCode = RequestedStatusCode();")
            endpoint.responses.forEachIndexed { index, response ->
                val condition =
                    if (index == 0) {
                        "requestedStatusCode == ${response.statusCode} || requestedStatusCode is null"
                    } else {
                        "requestedStatusCode == ${response.statusCode}"
                    }
                appendLine("        if ($condition)")
                appendLine("        {")
                append(renderResponseBlock(response))
                appendLine("        }")
            }
            append(renderResponseBlock(primaryResponse(endpoint.responses)))
        } else {
            append(renderResponseBlock(endpoint.responses.single()))
        }
        appendLine("    }")
    }

    private fun renderRequestBindingAssignment(
        binding: DotnetAspRequestBindingArtifact,
        requestSource: RequestSource,
    ): String = buildString {
        appendLine("        var ${bindingVariableName(binding.name)} = new ${binding.typeName}")
        appendLine("        {")
        binding.fields.forEachIndexed { index, field ->
            val suffix = if (index == binding.fields.lastIndex) "" else ","
            appendLine(
                "            ${field.name} = ${renderParsedFieldExpression(field, requestSource)}$suffix",
            )
        }
        appendLine("        };")
        appendLine("        _ = ${bindingVariableName(binding.name)};")
    }

    private fun renderHeadersBindingAssignment(binding: DotnetAspHeadersBindingArtifact): String = buildString {
        appendLine("        var ${bindingVariableName(binding.name)} = new ${binding.typeName}")
        appendLine("        {")
        binding.headers.forEachIndexed { index, header ->
            val suffix = if (index == binding.headers.lastIndex) "" else ","
            appendLine(
                "            ${header.name} = MicrosmithRequestParser.OptionalString(" +
                    "MicrosmithRequestParser.ReadHeaderValue(Request.Headers, ${escapeCsharpStringLiteral(header.headerName)}))$suffix",
            )
        }
        appendLine("        };")
        appendLine("        _ = ${bindingVariableName(binding.name)};")
    }

    private fun renderResponseBlock(response: DotnetAspResponseArtifact): String = buildString {
        response.headers.forEach { header ->
            appendLine(
                "            Response.Headers[${escapeCsharpStringLiteral(header.name)}] = " +
                    "${escapeCsharpStringLiteral(sampleHeaderValue(header.name))};",
            )
        }
        if (response.statusCode == 204) {
            appendLine("            return StatusCode(204);")
        } else {
            appendLine("            return StatusCode(${response.statusCode}, new ${response.model.typeName}());")
        }
    }

    private fun renderRequestedStatusHelper(): String = """
            private int? RequestedStatusCode()
            {
                if (!Request.Headers.TryGetValue("X-Microsmith-Response-Status", out var values))
                {
                    return null;
                }

                var candidate = values.Count > 0 ? values[0] : null;
                if (string.IsNullOrWhiteSpace(candidate))
                {
                    return null;
                }

                return int.TryParse(candidate, NumberStyles.Integer, CultureInfo.InvariantCulture, out var statusCode)
                    ? statusCode
                    : null;
            }
    """.trimIndent().prependIndent("    ") + "\n"

    private fun renderParsedFieldExpression(
        field: DotnetAspRequestFieldArtifact,
        requestSource: RequestSource,
    ): String {
        val rawExpression = when (requestSource) {
            RequestSource.PATH ->
                "MicrosmithRequestParser.ReadRouteValue(RouteData.Values, ${escapeCsharpStringLiteral(field.name)})"

            RequestSource.QUERY ->
                "MicrosmithRequestParser.ReadQueryValue(Request.Query, ${escapeCsharpStringLiteral(field.name)})"
        }
        val bindingName = "${requestSource.label}.${field.name}"
        val parsedExpression = when (val type = field.type) {
            DotnetFieldType.String ->
                if (field.optional || field.defaultValue != null) {
                    "MicrosmithRequestParser.OptionalString($rawExpression)"
                } else {
                    "MicrosmithRequestParser.RequireString($rawExpression, ${escapeCsharpStringLiteral(bindingName)})"
                }

            DotnetFieldType.Char -> renderScalarParse("Char", field, rawExpression, bindingName)
            DotnetFieldType.Byte -> renderScalarParse("Byte", field, rawExpression, bindingName)
            DotnetFieldType.SignedByte -> renderScalarParse("SignedByte", field, rawExpression, bindingName)
            DotnetFieldType.Short -> renderScalarParse("Short", field, rawExpression, bindingName)
            DotnetFieldType.UnsignedShort -> renderScalarParse("UnsignedShort", field, rawExpression, bindingName)
            DotnetFieldType.Int -> renderScalarParse("Int", field, rawExpression, bindingName)
            DotnetFieldType.UnsignedInt -> renderScalarParse("UnsignedInt", field, rawExpression, bindingName)
            DotnetFieldType.Long -> renderScalarParse("Long", field, rawExpression, bindingName)
            DotnetFieldType.UnsignedLong -> renderScalarParse("UnsignedLong", field, rawExpression, bindingName)
            DotnetFieldType.NativeInt -> renderScalarParse("NativeInt", field, rawExpression, bindingName)
            DotnetFieldType.UnsignedNativeInt -> renderScalarParse("UnsignedNativeInt", field, rawExpression, bindingName)
            DotnetFieldType.Float -> renderScalarParse("Float", field, rawExpression, bindingName)
            DotnetFieldType.Double -> renderScalarParse("Double", field, rawExpression, bindingName)
            DotnetFieldType.Decimal -> renderScalarParse("Decimal", field, rawExpression, bindingName)
            DotnetFieldType.Bool -> renderScalarParse("Bool", field, rawExpression, bindingName)
            DotnetFieldType.Guid -> renderScalarParse("Guid", field, rawExpression, bindingName)
            DotnetFieldType.DateOnly -> renderScalarParse("DateOnly", field, rawExpression, bindingName)
            DotnetFieldType.TimeOnly -> renderScalarParse("TimeOnly", field, rawExpression, bindingName)
            DotnetFieldType.DateTime -> renderScalarParse("DateTime", field, rawExpression, bindingName)
            DotnetFieldType.DateTimeOffset -> renderScalarParse("DateTimeOffset", field, rawExpression, bindingName)
            DotnetFieldType.TimeSpan -> renderScalarParse("TimeSpan", field, rawExpression, bindingName)
            is DotnetFieldType.Reference -> {
                val typeName = type.target
                if (field.optional || field.defaultValue != null) {
                    "MicrosmithRequestParser.OptionalJson<$typeName>($rawExpression, ${escapeCsharpStringLiteral(bindingName)})"
                } else {
                    "MicrosmithRequestParser.RequireJson<$typeName>($rawExpression, ${escapeCsharpStringLiteral(bindingName)})"
                }
            }
        }
        val defaultValue = field.defaultValue
        if (defaultValue == null) {
            return parsedExpression
        }
        return "($parsedExpression ?? ${renderDefaultExpression(field.type, defaultValue)})"
    }

    private fun renderScalarParse(
        parserSuffix: String,
        field: DotnetAspRequestFieldArtifact,
        rawExpression: String,
        bindingName: String,
    ): String {
        val mode = if (field.optional || field.defaultValue != null) "Optional" else "Require"
        return "MicrosmithRequestParser.$mode$parserSuffix($rawExpression, ${escapeCsharpStringLiteral(bindingName)})"
    }

    private fun renderDefaultExpression(type: DotnetFieldType, defaultValue: Any): String = when (type) {
        DotnetFieldType.String -> escapeCsharpStringLiteral(defaultValue.toString())
        DotnetFieldType.Char -> escapeCsharpCharLiteral(defaultValue.toString().first())
        DotnetFieldType.Byte,
        DotnetFieldType.SignedByte,
        DotnetFieldType.Short,
        DotnetFieldType.UnsignedShort,
        DotnetFieldType.Int,
        DotnetFieldType.NativeInt,
        -> defaultValue.toString()

        DotnetFieldType.UnsignedInt -> "${defaultValue}U"
        DotnetFieldType.Long -> "${defaultValue}L"
        DotnetFieldType.UnsignedLong -> "${defaultValue}UL"
        DotnetFieldType.UnsignedNativeInt -> "${defaultValue}U"
        DotnetFieldType.Float -> "${defaultValue.toString().ensureDecimal()}F"
        DotnetFieldType.Double -> "${defaultValue.toString().ensureDecimal()}D"
        DotnetFieldType.Decimal -> "${defaultValue.toString().ensureDecimal()}M"
        DotnetFieldType.Bool -> defaultValue.toString().lowercase(Locale.ROOT)
        DotnetFieldType.Guid ->
            "MicrosmithRequestParser.ParseGuidLiteral(${escapeCsharpStringLiteral(defaultValue.toString())})"

        DotnetFieldType.DateOnly ->
            "MicrosmithRequestParser.ParseDateOnlyLiteral(${escapeCsharpStringLiteral(defaultValue.toString())})"

        DotnetFieldType.TimeOnly ->
            "MicrosmithRequestParser.ParseTimeOnlyLiteral(${escapeCsharpStringLiteral(defaultValue.toString())})"

        DotnetFieldType.DateTime ->
            "MicrosmithRequestParser.ParseDateTimeLiteral(${escapeCsharpStringLiteral(defaultValue.toString())})"

        DotnetFieldType.DateTimeOffset ->
            "MicrosmithRequestParser.ParseDateTimeOffsetLiteral(${escapeCsharpStringLiteral(defaultValue.toString())})"

        DotnetFieldType.TimeSpan ->
            "MicrosmithRequestParser.ParseTimeSpanLiteral(${escapeCsharpStringLiteral(defaultValue.toString())})"

        is DotnetFieldType.Reference ->
            "MicrosmithRequestParser.ParseJsonLiteral<${type.target}>(${escapeCsharpStringLiteral(defaultValue.toString())})"
    }

    private fun renderModelPropertyType(type: DotnetFieldType): String = when (type) {
        is DotnetFieldType.Reference -> type.target
        else -> type.csharpType
    }

    private fun renderBindingPropertyType(field: DotnetAspRequestFieldArtifact): String {
        val baseType = renderModelPropertyType(field.type)
        return if (field.optional) "$baseType?" else baseType
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
        field.optional -> " = null;"
        defaultValue != null -> " = ${renderDefaultExpression(field.type, defaultValue)};"
        else -> renderInitializer(field.type)
    }
    }

    private fun primaryResponse(responses: List<DotnetAspResponseArtifact>): DotnetAspResponseArtifact =
        responses.firstOrNull { it.statusCode in 200..299 } ?: responses.first()

    private fun bindingVariableName(bindingName: String): String =
        bindingName.replaceFirstChar { char -> char.lowercase(Locale.ROOT) }

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

    private fun sampleHeaderValue(headerName: String): String =
        "sample-" + headerName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private fun String.ensureDecimal(): String = if (contains('.') || contains('E', ignoreCase = true)) this else "$this.0"

    private enum class RequestSource(val label: String) {
        PATH("path"),
        QUERY("query"),
    }
}
