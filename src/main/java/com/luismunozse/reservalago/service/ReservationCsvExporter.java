package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.Reservation;
import com.luismunozse.reservalago.model.ReservationVisitor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReservationCsvExporter {

    /**
     * Exporta reservas a una única hoja de Excel. Cada reserva se muestra en una fila
     * y, si tiene acompañantes, cada acompañante se agrega en filas consecutivas debajo,
     * con las columnas principales vacías salvo la fecha y nombre del acompañante.
     */
    public byte[] exportCsv(List<Reservation> reservations, boolean maskContacts) {
        String[] headers = {
                "Rol",                // Titular / Acompañante
                "Fecha de visita",
                "Estado",
                "Nombre",
                "Apellido",
                "DNI",
                "Email",
                "Teléfono",
                "Tipo de visitante",
                "Circuito",
                "Procedencia",
                "Adultos 18+",
                "Menores 2-17",
                "Bebés <2",
                "Movilidad reducida",
                "Alergias",
                "Creada"
        };

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Reservas");
            int rowIdx = 0;

            // Encabezados
            Row headerRow = sheet.createRow(rowIdx++);
            var headerStyle = workbook.createCellStyle();
            var headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

            // Estilos para distinguir titular / acompañante
            CellStyle mainStyle = workbook.createCellStyle();
            var mainFont = workbook.createFont();
            mainFont.setBold(true);
            mainStyle.setFont(mainFont);
            mainStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            mainStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle visitorStyle = workbook.createCellStyle();
            visitorStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
            visitorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (Reservation r : reservations) {
                // Fila del titular
                Row mainRow = sheet.createRow(rowIdx++);
                fillMainRow(mainRow, r, dateFormatter, maskContacts, mainStyle);

                // Filas de acompañantes debajo
                if (r.getVisitors() != null && !r.getVisitors().isEmpty()) {
                    for (ReservationVisitor v : r.getVisitors()) {
                        Row visitorRow = sheet.createRow(rowIdx++);
                        fillVisitorRow(visitorRow, r, v, dateFormatter, maskContacts, visitorStyle);
                    }
                }
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error generando archivo Excel de reservas", e);
        }
    }

    private static void fillMainRow(Row row, Reservation r, DateTimeFormatter dateFormatter, boolean maskContacts, CellStyle style) {
        int col = 0;
        set(row, col++, "Titular", style);
        set(row, col++, r.getVisitDate() != null ? r.getVisitDate().format(dateFormatter) : "", style);
        set(row, col++, r.getStatus() != null ? r.getStatus().name() : "", style);
        set(row, col++, n(r.getFirstName()), style);
        set(row, col++, n(r.getLastName()), style);
        set(row, col++, maskContacts ? mask(r.getDni()) : n(r.getDni()), style);
        set(row, col++, maskContacts ? mask(r.getEmail()) : n(r.getEmail()), style);
        set(row, col++, maskContacts ? mask(r.getPhone()) : n(r.getPhone()), style);
        set(row, col++, r.getVisitorType() != null ? r.getVisitorType().name() : "", style);
        set(row, col++, r.getCircuit() != null ? r.getCircuit().name() : "", style);
        set(row, col++, n(r.getOriginLocation()), style);
        set(row, col++, String.valueOf(r.getAdults18Plus()), style);
        set(row, col++, String.valueOf(r.getChildren2To17()), style);
        set(row, col++, String.valueOf(r.getBabiesLessThan2()), style);
        set(row, col++, String.valueOf(r.getReducedMobility()), style);
        set(row, col++, String.valueOf(r.getAllergies()), style);
        set(row, col, r.getCreatedAt() != null ? r.getCreatedAt().toString() : "", style);
    }

    private static void fillVisitorRow(Row row, Reservation r, ReservationVisitor v, DateTimeFormatter dateFormatter, boolean maskContacts, CellStyle style) {
        int col = 0;
        set(row, col++, "Acompañante", style);
        set(row, col++, r.getVisitDate() != null ? r.getVisitDate().format(dateFormatter) : "", style);
        set(row, col++, "", style); // Estado
        set(row, col++, n(v.getFirstName()), style);
        set(row, col++, n(v.getLastName()), style);
        set(row, col++, maskContacts ? mask(v.getDni()) : n(v.getDni()), style);
        set(row, col++, "", style); // Email no se repite
        set(row, col++, "", style); // Teléfono no se repite
        set(row, col++, "Acompañante", style); // Tipo de visitante
        set(row, col++, r.getCircuit() != null ? r.getCircuit().name() : "", style);
        set(row, col++, n(r.getOriginLocation()), style);
        set(row, col++, "", style);
        set(row, col++, "", style);
        set(row, col++, "", style);
        set(row, col++, "", style);
        set(row, col++, "", style);
        set(row, col, "", style);
    }

    private static void set(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }

    private static String mask(String s) {
        if (s == null || s.length() < 4) return "***";
        int keep = Math.min(3, s.length());
        return "***" + s.substring(s.length() - keep);
    }
}
