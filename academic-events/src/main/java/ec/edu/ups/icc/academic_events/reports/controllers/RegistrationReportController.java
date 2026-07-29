package ec.edu.ups.icc.academic_events.reports.controllers;

import ec.edu.ups.icc.academic_events.reports.dtos.ReportFileDTO;
import ec.edu.ups.icc.academic_events.reports.services.RegistrationReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class RegistrationReportController {

    private final RegistrationReportService reportService;

    @GetMapping(
            value = "/events/{eventId}/registrations/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'ORGANIZER')"
    )
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        ReportFileDTO report =
                reportService.generatePdf(
                        eventId,
                        authentication
                );

        return buildResponse(report);
    }

    @GetMapping(
            value = "/events/{eventId}/registrations/excel",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'ORGANIZER')"
    )
    public ResponseEntity<byte[]> downloadExcel(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        ReportFileDTO report =
                reportService.generateExcel(
                        eventId,
                        authentication
                );

        return buildResponse(report);
    }

    private ResponseEntity<byte[]> buildResponse(
            ReportFileDTO report
    ) {
        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(
                                report.fileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .contentType(
                        MediaType.parseMediaType(
                                report.contentType()
                        )
                )
                .contentLength(
                        report.content().length
                )
                .body(report.content());
    }
}