package io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics

import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import kotlin.reflect.KClass

@InternalMicrosmithApi
public class ProtobufResolutionDiagnosticMapper : ResolutionIssueDiagnosticMapper<ProtobufResolutionIssue> {
    override val issueType: KClass<ProtobufResolutionIssue> = ProtobufResolutionIssue::class

    override fun map(issue: ProtobufResolutionIssue): ResolutionDiagnostic =
        when (issue) {
            is ProtobufResolutionIssue.ReferenceIssue -> issue.toDiagnostic()

            is ProtobufResolutionIssue.DeclarationIssue -> issue.toDiagnostic()

            is ProtobufResolutionIssue.EnumIssue -> issue.toDiagnostic()

            is ProtobufResolutionIssue.ReservationIssue -> issue.toDiagnostic()
        }
}
