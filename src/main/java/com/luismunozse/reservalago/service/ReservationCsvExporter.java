package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.Reservation;
import org.apache.poi.ss.usermodel.Cell;
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

    public byte[] exportCsv(List<Reservation> reservations, boolean maskContacts) {
        String[] headers = {
                "id","visit_date","first_name","last_name","dni","phone","email",
                "visitor_type","institution_name","institution_students",
                "adults_18_plus","children_2_to_17","babies_less_than_2","reduced_mobility","allergies",
                "origin_location","how_heard","status","created_at","updated_at"
        };

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Reservas");
            int rowIdx = 0;

            // Header
            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

            // Data reservas
            for (Reservation r : reservations) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;

                row.createCell(col++).setCellValue(r.getId() != null ? r.getId().toString() : "");
                row.createCell(col++).setCellValue(r.getVisitDate() != null ? r.getVisitDate().format(dateFormatter) : "");
                row.createCell(col++).setCellValue(n(r.getFirstName()));
                row.createCell(col++).setCellValue(n(r.getLastName()));
                row.createCell(col++).setCellValue(maskContacts ? mask(r.getDni()) : n(r.getDni()));
                row.createCell(col++).setCellValue(maskContacts ? mask(r.getPhone()) : n(r.getPhone()));
                row.createCell(col++).setCellValue(maskContacts ? mask(r.getEmail()) : n(r.getEmail()));
                row.createCell(col++).setCellValue(r.getVisitorType() != null ? r.getVisitorType().name() : "");
                row.createCell(col++).setCellValue(n(r.getInstitutionName()));
                row.createCell(col++).setCellValue(r.getInstitutionStudents() != null ? r.getInstitutionStudents() : 0);
                row.createCell(col++).setCellValue(r.getAdults18Plus());
                row.createCell(col++).setCellValue(r.getChildren2To17());
                row.createCell(col++).setCellValue(r.getBabiesLessThan2());
                row.createCell(col++).setCellValue(r.getReducedMobility());
                row.createCell(col++).setCellValue(r.getAllergies());
                row.createCell(col++).setCellValue(n(r.getOriginLocation()));
                row.createCell(col++).setCellValue(r.getHowHeard() != null ? r.getHowHeard().name() : "");
                row.createCell(col++).setCellValue(r.getStatus() != null ? r.getStatus().name() : "");
                row.createCell(col++).setCellValue(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
                row.createCell(col++).setCellValue(r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : "");
            }

            // Auto-size columnas básicas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Hoja de visitantes
            Sheet visitorsSheet = workbook.createSheet("Visitantes");
            int vRowIdx = 0;
            Row vHeader = visitorsSheet.createRow(vRowIdx++);
            vHeader.createCell(0).setCellValue("reservation_id");
            vHeader.createCell(1).setCellValue("first_name");
            vHeader.createCell(2).setCellValue("last_name");
            vHeader.createCell(3).setCellValue("dni");

            for (Reservation r : reservations) {
                if (r.getVisitors() == null || r.getVisitors().isEmpty()) {
                    continue;
                }
                for (com.luismunozse.reservalago.model.ReservationVisitor v : r.getVisitors()) {
                    Row vr = visitorsSheet.createRow(vRowIdx++);
                    int c = 0;
                    vr.createCell(c++).setCellValue(r.getId() != null ? r.getId().toString() : "");
                    vr.createCell(c++).setCellValue(n(v.getFirstName()));
                    vr.createCell(c++).setCellValue(n(v.getLastName()));
                    String dniValue = maskContacts ? mask(v.getDni()) : n(v.getDni());
                    vr.createCell(c).setCellValue(dniValue);
                }
            }

            for (int i = 0; i < 4; i++) {
                visitorsSheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error generando archivo Excel de reservas", e);
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
