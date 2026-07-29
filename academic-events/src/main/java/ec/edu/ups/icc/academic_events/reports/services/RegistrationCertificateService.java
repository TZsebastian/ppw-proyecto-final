package ec.edu.ups.icc.academic_events.reports.services;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import ec.edu.ups.icc.academic_events.registrations.entities.RegistrationEntity;
import ec.edu.ups.icc.academic_events.registrations.repositories.RegistrationRepository;
import ec.edu.ups.icc.academic_events.reports.dtos.ReportFileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.lowagie.text.Image;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class RegistrationCertificateService {

    private static final String CONFIRMED_STATUS = "CONFIRMED";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "dd 'de' MMMM 'de' yyyy",
            Locale.of("es", "EC"));

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm",
            Locale.of("es", "EC"));

    private final RegistrationRepository registrationRepository;

    @Transactional(readOnly = true)
    public ReportFileDTO generateCertificate(
            Long registrationId,
            Authentication authentication) {
        RegistrationEntity registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inscripción no encontrada"));

        validatePermission(
                registration,
                authentication);

        validateConfirmedStatus(registration);

        byte[] content = buildCertificate(registration);

        return new ReportFileDTO(
                content,
                buildFileName(registration),
                "application/pdf");
    }

    private byte[] buildCertificate(
            RegistrationEntity registration) {
        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(
                    PageSize.A4.rotate(),
                    45,
                    45,
                    30,
                    30);

            PdfWriter.getInstance(
                    document,
                    outputStream);

            document.open();

            addCertificateContent(
                    document,
                    registration);

            document.close();

            return outputStream.toByteArray();

        } catch (DocumentException | IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo generar el certificado de inscripción",
                    exception);
        }
    }

    private void addCertificateContent(
            Document document,
            RegistrationEntity registration) throws DocumentException {

        try {
            ClassPathResource logoResource = new ClassPathResource(
                    "static/images/logo-ups.png");

            try (InputStream inputStream = logoResource.getInputStream()) {

                Image logo = Image.getInstance(
                        inputStream.readAllBytes());

                logo.scaleToFit(90, 60);
                logo.setAlignment(Element.ALIGN_CENTER);
                logo.setSpacingAfter(6);

                document.add(logo);
            }

        } catch (Exception exception) {

        }

        Font institutionFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                12);

        Font certificateTitleFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                24);

        Font bodyFont = FontFactory.getFont(
                FontFactory.HELVETICA,
                13);

        Font participantFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                19);

        Font eventFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                16);

        Font footerFont = FontFactory.getFont(
                FontFactory.HELVETICA,
                8);

        Paragraph institution = new Paragraph(
                "UNIVERSIDAD POLITÉCNICA SALESIANA",
                institutionFont);

        institution.setAlignment(Element.ALIGN_CENTER);
        institution.setSpacingAfter(8);
        document.add(institution);

        Paragraph title = new Paragraph(
                "CERTIFICADO DE INSCRIPCIÓN",
                certificateTitleFont);

        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(16);
        document.add(title);

        Paragraph certificationText = new Paragraph(
                "Se certifica que:",
                bodyFont);

        certificationText.setAlignment(Element.ALIGN_CENTER);
        certificationText.setSpacingAfter(8);
        document.add(certificationText);

        Paragraph participantName = new Paragraph(
                buildParticipantName(registration),
                participantFont);

        participantName.setAlignment(Element.ALIGN_CENTER);
        participantName.setSpacingAfter(8);
        document.add(participantName);

        Paragraph participantEmail = new Paragraph(
                registration.getParticipant().getEmail(),
                bodyFont);

        participantEmail.setAlignment(Element.ALIGN_CENTER);
        participantEmail.setSpacingAfter(12);
        document.add(participantEmail);

        Paragraph participationText = new Paragraph(
                "se encuentra oficialmente inscrito/a en el evento académico:",
                bodyFont);

        participationText.setAlignment(Element.ALIGN_CENTER);
        participationText.setSpacingAfter(8);
        document.add(participationText);

        Paragraph eventTitle = new Paragraph(
                registration.getEvent().getTitle(),
                eventFont);

        eventTitle.setAlignment(Element.ALIGN_CENTER);
        eventTitle.setSpacingAfter(12);
        document.add(eventTitle);

        String eventDate = registration.getEvent().getStartAt() != null
                ? registration.getEvent()
                        .getStartAt()
                        .format(DATE_FORMATTER)
                : "Fecha no disponible";

        Paragraph eventInformation = new Paragraph(
                "Fecha del evento: " + eventDate
                        + "\nModalidad: "
                        + registration.getEvent().getModality(),
                bodyFont);

        eventInformation.setAlignment(Element.ALIGN_CENTER);
        eventInformation.setLeading(17);
        eventInformation.setSpacingAfter(14);
        document.add(eventInformation);

        String confirmedAt = registration.getConfirmedAt() != null
                ? registration.getConfirmedAt()
                        .format(DATE_TIME_FORMATTER)
                : "-";

        Paragraph confirmationInformation = new Paragraph(
                "Inscripción confirmada el: "
                        + confirmedAt,
                bodyFont);

        confirmationInformation.setAlignment(
                Element.ALIGN_CENTER);

        confirmationInformation.setSpacingAfter(14);
        document.add(confirmationInformation);

        Paragraph code = new Paragraph(
                "Código de verificación: "
                        + registration.getRegistrationCode(),
                footerFont);

        code.setAlignment(Element.ALIGN_CENTER);
        code.setSpacingAfter(4);
        document.add(code);

        Paragraph generatedAt = new Paragraph(
                "Documento generado el "
                        + LocalDateTime.now()
                                .format(DATE_TIME_FORMATTER),
                footerFont);

        generatedAt.setAlignment(Element.ALIGN_CENTER);
        document.add(generatedAt);
    }

    private void validateConfirmedStatus(
            RegistrationEntity registration) {
        if (!CONFIRMED_STATUS.equals(
                registration.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El certificado solo puede generarse para una inscripción confirmada");
        }

        if (registration.getConfirmedAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La inscripción confirmada no tiene fecha de confirmación");
        }
    }

    private void validatePermission(
            RegistrationEntity registration,
            Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado");
        }

        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(
                        authority.getAuthority()));

        if (isAdmin) {
            return;
        }

        String authenticatedEmail = authentication.getName();

        String participantEmail = registration.getParticipant().getEmail();

        if (participantEmail.equalsIgnoreCase(
                authenticatedEmail)) {
            return;
        }

        boolean isOrganizer = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> "ROLE_ORGANIZER".equals(
                        authority.getAuthority()));

        String organizerEmail = registration.getEvent()
                .getOrganizer()
                .getEmail();

        if (isOrganizer
                && organizerEmail.equalsIgnoreCase(
                        authenticatedEmail)) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tiene permisos para descargar este certificado");
    }

    private String buildParticipantName(
            RegistrationEntity registration) {
        return registration.getParticipant().getFirstName()
                + " "
                + registration.getParticipant().getLastName();
    }

    private String buildFileName(
            RegistrationEntity registration) {
        String participantName = buildParticipantName(registration)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (participantName.isBlank()) {
            participantName = "participante-"
                    + registration
                            .getParticipant()
                            .getId();
        }

        return "certificado-inscripcion-"
                + participantName
                + "-"
                + registration.getId()
                + ".pdf";
    }
}