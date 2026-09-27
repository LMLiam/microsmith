package io.github.lmliam.microsmith.resolve.diagnostics

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.resolve.ResolutionIssue
import kotlin.reflect.KClass

@ServiceContract
interface ResolutionIssueDiagnosticMapper<I : ResolutionIssue> {
    val issueType: KClass<I>

    fun map(issue: I): ResolutionDiagnostic
}
