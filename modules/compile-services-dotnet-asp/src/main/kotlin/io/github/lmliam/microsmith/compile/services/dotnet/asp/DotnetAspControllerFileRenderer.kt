package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType

internal object DotnetAspControllerFileRenderer {
    fun renderControllerBaseFile(artifact: DotnetAspServiceArtifact): String = CSharp.render(
        CSharp.file(controllersNamespace(artifact)) {
            using(contractsNamespace(artifact))
            using(ASP_NET_MVC_NAMESPACE)
            using(SYSTEM_NAMESPACE)
            using(SYSTEM_THREADING_NAMESPACE)
            using(SYSTEM_TASKS_NAMESPACE)
            classType(
                name = controllerBaseTypeName(artifact),
                modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
                baseTypes = listOf(csharpType(MICROSMITH_CONTROLLER_BASE_TYPE_NAME)),
                attributes = listOf(CSharp.attribute(API_CONTROLLER_ATTRIBUTE)),
            ) {
                artifact.endpoints.forEach { endpoint ->
                    addMember(renderActionMethod(endpoint))
                }
                artifact.endpoints.forEach { endpoint ->
                    addMember(renderAbstractHandler(endpoint))
                }
                artifact.endpoints.forEach { endpoint ->
                    addMember(renderResultMapper(endpoint))
                }
            }
        },
    )
}

internal const val ACTION_RESULT_TYPE_NAME = "ActionResult"
internal const val API_CONTROLLER_ATTRIBUTE = "ApiController"
internal const val FROM_BODY_ATTRIBUTE = "FromBody"
internal const val FROM_QUERY_ATTRIBUTE = "FromQuery"
internal const val FROM_ROUTE_ATTRIBUTE = "FromRoute"
internal const val PRODUCES_RESPONSE_TYPE_ATTRIBUTE = "ProducesResponseType"
internal const val VOID_TYPE_NAME = "void"
private const val ASP_NET_MVC_NAMESPACE = "Microsoft.AspNetCore.Mvc"
private const val SYSTEM_NAMESPACE = "System"
private const val SYSTEM_TASKS_NAMESPACE = "System.Threading.Tasks"
private const val SYSTEM_THREADING_NAMESPACE = "System.Threading"
