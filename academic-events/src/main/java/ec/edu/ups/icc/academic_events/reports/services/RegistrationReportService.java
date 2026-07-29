package ec.edu.ups.icc.academic_events.reports.services;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ec.edu.ups.icc.academic_events.events.entities.EventEntity;
import ec.edu.ups.icc.academic_events.events.repositories.EventRepository;
import ec.edu.ups.icc.academic_events.registrations.entities.RegistrationEntity;
import ec.edu.ups.icc.academic_events.registrations.repositories.RegistrationRepository;
import ec.edu.ups.icc.academic_events.reports.dtos.RegistrationReportRowDTO;
import ec.edu.ups.icc.academic_events.reports.dtos.ReportFileDTO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RegistrationReportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm",
            new Locale("es", "EC"));

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    @Transactional(readOnly = true)
    public ReportFileDTO generatePdf(
            Long eventId,
            Authentication authentication) {
        EventEntity event = findEvent(eventId);

        validatePermission(event, authentication);

        List<RegistrationReportRowDTO> rows = loadRows(eventId);

        byte[] content = buildPdf(event, rows);

        return new ReportFileDTO(
                content,
                buildFileName(event, "pdf"),
                "application/pdf");
    }

    @Transactional(readOnly = true)
    public ReportFileDTO generateExcel(
            Long eventId,
            Authentication authentication) {
        EventEntity event = findEvent(eventId);

        validatePermission(event, authentication);

        List<RegistrationReportRowDTO> rows = loadRows(eventId);

        byte[] content = buildExcel(event, rows);

        return new ReportFileDTO(
                content,
                buildFileName(event, "xlsx"),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private EventEntity findEvent(Long eventId) {
        return eventRepository
                .findByIdAndDeletedFalse(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evento no encontrado"));
    }

    private List<RegistrationReportRowDTO> loadRows(
            Long eventId) {
        return registrationRepository
                .findAllByEventId(eventId)
                .stream()
                .map(this::toReportRow)
                .toList();
    }

    private RegistrationReportRowDTO toReportRow(
            RegistrationEntity registration) {
        String participantName = registration.getParticipant().getFirstName()
                + " "
                + registration.getParticipant().getLastName();

        return new RegistrationReportRowDTO(
                registration.getId(),
                registration.getRegistrationCode(),
                participantName,
                registration.getParticipant().getEmail(),
                registration.getStatus(),
                registration.getRegisteredAt(),
                registration.getConfirmedAt(),
                registration.getCancelledAt());
    }

    private byte[] buildPdf(
            EventEntity event,
            List<RegistrationReportRowDTO> rows) {
        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());

            PdfWriter.getInstance(
                    document,
                    outputStream);

            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    16);

            Font subtitleFont = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    10);

            Paragraph title = new Paragraph(
                    "Reporte de inscripciones",
                    titleFont);

            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph eventTitle = new Paragraph(
                    "Evento: " + event.getTitle(),
                    subtitleFont);

            eventTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(eventTitle);

            document.add(
                    new Paragraph(
                            "Generado: "
                                    + formatDateTime(
                                            LocalDateTime.now()),
                            subtitleFont));

            document.add(
                    new Paragraph(
                            "Total de inscripciones: "
                                    + rows.size(),
                            subtitleFont));

            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);

            table.setWidthPercentage(100);
            table.setWidths(
                    new float[] {
                            1.2f,
                            2.7f,
                            3.2f,
                            1.5f,
                            1.8f,
                            1.8f,
                            1.8f
                    });

            addPdfHeader(
                    table,
                    "ID",
                    "Participante",
                    "Correo",
                    "Estado",
                    "Registro",
                    "Confirmación",
                    "Cancelación");

            for (RegistrationReportRowDTO row : rows) {
                addPdfCell(
                        table,
                        String.valueOf(row.registrationId()));

                addPdfCell(
                        table,
                        row.participantName());

                addPdfCell(
                        table,
                        row.participantEmail());

                addPdfCell(
                        table,
                        row.status());

                addPdfCell(
                        table,
                        formatDateTime(row.registeredAt()));

                addPdfCell(
                        table,
                        formatDateTime(row.confirmedAt()));

                addPdfCell(
                        table,
                        formatDateTime(row.cancelledAt()));
            }

            document.add(table);
            document.close();

            return outputStream.toByteArray();

        } catch (DocumentException | IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo generar el reporte PDF",
                    exception);
        }
    }

    private byte[] buildExcel(
            EventEntity event,
            List<RegistrationReportRowDTO> rows) {
        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inscripciones");

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();

            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            CellStyle titleStyle = workbook.createCellStyle();

            titleStyle.setFont(titleFont);

            Row titleRow = sheet.createRow(0);

            titleRow.createCell(0)
                    .setCellValue(
                            "Reporte de inscripciones");

            titleRow.getCell(0)
                    .setCellStyle(titleStyle);

            Row eventRow = sheet.createRow(1);

            eventRow.createCell(0)
                    .setCellValue(
                            "Evento: " + event.getTitle());

            Row generatedRow = sheet.createRow(2);

            generatedRow.createCell(0)
                    .setCellValue(
                            "Generado: "
                                    + formatDateTime(
                                            LocalDateTime.now()));

            Row totalRow = sheet.createRow(3);

            totalRow.createCell(0)
                    .setCellValue(
                            "Total: " + rows.size());

            CellStyle headerStyle = workbook.createCellStyle();

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();

            headerFont.setBold(true);
            headerFont.setUnderline(
                    FontUnderline.SINGLE.getByteValue());

            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(
                    HorizontalAlignment.CENTER);

            headerStyle.setVerticalAlignment(
                    VerticalAlignment.CENTER);

            Row headerRow = sheet.createRow(5);

            String[] headers = {
                    "ID",
                    "Código",
                    "Participante",
                    "Correo",
                    "Estado",
                    "Fecha de registro",
                    "Fecha de confirmación",
                    "Fecha de cancelación"
            };

            for (int column = 0; column < headers.length; column++) {

                headerRow.createCell(column)
                        .setCellValue(headers[column]);

                headerRow.getCell(column)
                        .setCellStyle(headerStyle);
            }

            int rowIndex = 6;

            for (RegistrationReportRowDTO row : rows) {
                Row excelRow = sheet.createRow(rowIndex++);

                excelRow.createCell(0)
                        .setCellValue(
                                row.registrationId());

                excelRow.createCell(1)
                        .setCellValue(
                                row.registrationCode().toString());

                excelRow.createCell(2)
                        .setCellValue(
                                row.participantName());

                excelRow.createCell(3)
                        .setCellValue(
                                row.participantEmail());

                excelRow.createCell(4)
                        .setCellValue(
                                row.status());

                excelRow.createCell(5)
                        .setCellValue(
                                formatDateTime(
                                        row.registeredAt()));

                excelRow.createCell(6)
                        .setCellValue(
                                formatDateTime(
                                        row.confirmedAt()));

                excelRow.createCell(7)
                        .setCellValue(
                                formatDateTime(
                                        row.cancelledAt()));
            }

            for (int column = 0; column < headers.length; column++) {

                sheet.autoSizeColumn(column);

                int currentWidth = sheet.getColumnWidth(column);

                sheet.setColumnWidth(
                        column,
                        Math.min(
                                currentWidth + 1000,
                                15000));
            }

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo generar el reporte Excel",
                    exception);
        }
    }

    private void addPdfHeader(
            PdfPTable table,
            String... headers) {
        Font font = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                9);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(
                    new Phrase(header, font));

            cell.setHorizontalAlignment(
                    Element.ALIGN_CENTER);

            cell.setVerticalAlignment(
                    Element.ALIGN_MIDDLE);

            cell.setPadding(6);

            table.addCell(cell);
        }
    }

    private void addPdfCell(
            PdfPTable table,
            String value) {
        PdfPCell cell = new PdfPCell(
                new Phrase(
                        value != null ? value : "-",
                        FontFactory.getFont(
                                FontFactory.HELVETICA,
                                8)));

        cell.setPadding(5);
        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE);

        table.addCell(cell);
    }

    private String formatDateTime(
            LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.format(DATE_TIME_FORMATTER)
                : "-";
    }

    private String buildFileName(
            EventEntity event,
            String extension) {
        String normalizedTitle = event.getTitle()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (normalizedTitle.isBlank()) {
            normalizedTitle = "evento-" + event.getId();
        }

        return "inscripciones-"
                + normalizedTitle
                + "."
                + extension;
    }

    private void validatePermission(
            EventEntity event,
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

        boolean isOrganizer = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> "ROLE_ORGANIZER".equals(
                        authority.getAuthority()));

        if (!isOrganizer) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permisos para generar reportes");
        }

        String organizerEmail = event.getOrganizer().getEmail();

        if (!organizerEmail.equalsIgnoreCase(
                authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el propietario del evento puede generar sus reportes");
        }
    }
}