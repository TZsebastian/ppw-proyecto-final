package ec.edu.ups.icc.academic_events.reports.controllers;

import ec.edu.ups.icc.academic_events.reports.dtos.ReportFileDTO;
import ec.edu.ups.icc.academic_events.reports.services.RegistrationCertificateService;
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
public class RegistrationCertificateController {

    private final RegistrationCertificateService certificateService;

    @GetMapping(
            value = "/registrations/{registrationId}/certificate",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'ORGANIZER', 'PARTICIPANT')"
    )
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Long registrationId,
            Authentication authentication
    ) {
        ReportFileDTO certificate =
                certificateService.generateCertificate(
                        registrationId,
                        authentication
                );

        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(
                                certificate.fileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(
                        certificate.content().length
                )
                .body(certificate.content());
    }
}