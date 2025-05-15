package com.example.hcm25_cpl_ks_java_01_lms.role;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RoleExcelImporter {

    public static List<Role> importRoles(InputStream inputStream) throws IOException {
        List<Role> roles = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);

        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();

        int rowNumber = 0;
        while (rows.hasNext()) {
            Row currentRow = rows.next();
            // Skip header
            if (rowNumber == 0) {
                rowNumber++;
                continue;
            }

            Iterator<Cell> cellsInRow = currentRow.iterator();

            Role role = new Role();
            int cellIdx = 0;
            while (cellsInRow.hasNext()) {
                Cell currentCell = cellsInRow.next();
                switch (cellIdx) {
                    case 0:
//                        role.setId((long) currentCell.getNumericCellValue());
                        break;
                    case 1:
                        role.setName(currentCell.getStringCellValue());
                        break;
                    default:
                        break;
                }
                cellIdx++;
            }

            roles.add(role);
        }

        workbook.close();
        return roles;
    }
}

