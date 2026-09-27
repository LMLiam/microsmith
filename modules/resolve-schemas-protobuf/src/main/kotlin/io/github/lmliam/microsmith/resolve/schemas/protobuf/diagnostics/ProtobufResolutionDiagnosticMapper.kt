package io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics
import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

@ServiceProvider(ResolutionIssueDiagnosticMapper::class)
class ProtobufResolutionDiagnosticMapper : ResolutionIssueDiagnosticMapper<ProtobufResolutionIssue> {
    override val issueType =
        ProtobufResolutionIssue::class

    override fun map(issue: ProtobufResolutionIssue): ResolutionDiagnostic = when (issue) {
        is ProtobufResolutionIssue.ReferenceIssue ->
            issue.toDiagnostic()

        is ProtobufResolutionIssue.DeclarationIssue ->
            issue.toDiagnostic()

        is ProtobufResolutionIssue.EnumIssue ->
            issue.toDiagnostic()

        is ProtobufResolutionIssue.ReservationIssue ->
            issue.toDiagnostic()
    }
}
