package io.github.lmliam.microsmith.cli.init

internal object DotnetBootstrapScriptTemplateRenderer {
    fun render(profile: OnboardingProfile): String = buildString {
        appendHeader(profile)
        appendLine(scriptBody())
    }

    private fun StringBuilder.appendHeader(profile: OnboardingProfile) {
        appendLine("// Bootstrapped Microsmith ASP.NET service generation")
        appendLine("// for this ${profile.bootstrapTargetDescription}.")
        appendLine("// Canonical first run:")
        appendLine("// microsmith run build.microsmith.kts")
        profile.recommendedOutputDirectory?.let { outputDirectory ->
            appendLine("// Common repository-native output path:")
            appendLine("// microsmith run build.microsmith.kts --out $outputDirectory")
        }
    }

    private fun scriptBody(): String = buildString {
        appendLine("microsmith {")
        appendServicesBlock()
        appendLine("}")
    }

    private fun StringBuilder.appendServicesBlock() {
        appendLine("    services {")
        appendDotnetDefaults()
        appendUserService()
        appendLine("    }")
    }

    private fun StringBuilder.appendDotnetDefaults() {
        appendLine("        dotnet {")
        appendLine("            target(NET8)")
        appendLine("            solutions {")
        appendLine("                \"Platform\" { }")
        appendLine("            }")
        appendLine("        }")
        appendLine()
    }

    private fun StringBuilder.appendUserService() {
        appendLine("        \"UserService\" {")
        appendLine("            dotnet {")
        appendLine("                solution(\"Platform\")")
        appendLine("                project(\"UserService.Api\")")
        appendModelsBlock()
        appendAspBlock()
        appendLine("            }")
        appendLine("        }")
    }

    private fun StringBuilder.appendModelsBlock() {
        appendLine("                models {")
        appendLine("                    \"User\" {")
        appendLine("                        string(\"id\")")
        appendLine("                        string(\"email\")")
        appendLine("                    }")
        appendLine("                    \"Problem\" {")
        appendLine("                        string(\"detail\")")
        appendLine("                    }")
        appendLine("                    \"Report\" {")
        appendLine("                        string(\"id\")")
        appendLine("                        string(\"title\")")
        appendLine("                    }")
        appendLine("                }")
    }

    private fun StringBuilder.appendAspBlock() {
        appendLine("                asp {")
        appendLine("                    rest {")
        appendUsersEndpoints()
        appendReportsEndpoints()
        appendLine("                    }")
        appendLine("                }")
    }

    private fun StringBuilder.appendUsersEndpoints() {
        appendLine("                        \"/users\" {")
        appendLine("                            get(\"/{id}\", \"GetUser\") {")
        appendLine("                                path(\"GetUserPath\") {")
        appendLine("                                    string(\"id\")")
        appendLine("                                }")
        appendLine("                                query(\"GetUserQuery\") {")
        appendLine("                                    bool(\"includeDetails\") {")
        appendLine("                                        optional()")
        appendLine("                                        default(false)")
        appendLine("                                    }")
        appendLine("                                }")
        appendLine("                                headers(\"GetUserHeaders\") {")
        appendLine("                                    header(\"X-Correlation-Id\")")
        appendLine("                                }")
        appendLine("                                responses {")
        appendLine("                                    ok(\"User\") {")
        appendLine("                                        headers {")
        appendLine("                                            header(\"ETag\")")
        appendLine("                                        }")
        appendLine("                                    }")
        appendLine("                                    notFound(\"Problem\")")
        appendLine("                                }")
        appendLine("                            }")
        appendLine()
        appendLine("                            post(\"CreateUser\") {")
        appendLine("                                body(\"CreateUserBody\") {")
        appendLine("                                    string(\"email\")")
        appendLine("                                }")
        appendLine("                                responses {")
        appendLine("                                    created(\"User\") {")
        appendLine("                                        headers {")
        appendLine("                                            header(\"Location\")")
        appendLine("                                        }")
        appendLine("                                    }")
        appendLine("                                    badRequest(\"Problem\")")
        appendLine("                                }")
        appendLine("                            }")
        appendLine("                        }")
        appendLine()
    }

    private fun StringBuilder.appendReportsEndpoints() {
        appendLine("                        \"/reports\" {")
        appendLine("                            get(\"/{reportId}\", \"GetReport\") {")
        appendLine("                                path(\"GetReportPath\") {")
        appendLine("                                    guid(\"reportId\")")
        appendLine("                                }")
        appendLine("                                query(\"GetReportQuery\") {")
        appendLine("                                    int(\"days\")")
        appendLine("                                    dateOnly(\"since\")")
        appendLine("                                    dateTimeOffset(\"requestedAt\")")
        appendLine("                                    decimal(\"threshold\") {")
        appendLine("                                        optional()")
        appendLine("                                        default(1.5)")
        appendLine("                                    }")
        appendLine("                                    timeSpan(\"window\") {")
        appendLine("                                        optional()")
        appendLine("                                    }")
        appendLine("                                }")
        appendLine("                                responses {")
        appendLine("                                    ok(\"Report\")")
        appendLine("                                }")
        appendLine("                            }")
        appendLine("                        }")
    }
}
