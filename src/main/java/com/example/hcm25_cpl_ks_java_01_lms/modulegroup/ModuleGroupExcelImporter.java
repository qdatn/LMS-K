package com.example.hcm25_cpl_ks_java_01_lms.modulegroup;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ModuleGroupExcelImporter {
    public static List<ModuleGroup> importModuleGroups(InputStream inputStream) throws IOException {
        List<ModuleGroup> moduleGroups = new ArrayList<>();
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

            ModuleGroup moduleGroup = new ModuleGroup();
            int cellIdx = 0;
            while (cellsInRow.hasNext()) {
                Cell currentCell = cellsInRow.next();
                switch (cellIdx) {
                    case 0:
                        break;// skip Id because jpa not accept entity have id when create
                    case 1:
                        moduleGroup.setName(currentCell.getStringCellValue());
                        break;
                    default:
                        break;
                }
                cellIdx++;
            }

            moduleGroups.add(moduleGroup);
        }

        workbook.close();
        return moduleGroups;
    }
}
