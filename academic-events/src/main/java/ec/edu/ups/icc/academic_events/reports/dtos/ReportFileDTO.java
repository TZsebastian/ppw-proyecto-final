package ec.edu.ups.icc.academic_events.reports.dtos;

public record ReportFileDTO(
        byte[] content,
        String fileName,
        String contentType
) {
}