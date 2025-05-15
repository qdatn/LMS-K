package com.example.hcm25_cpl_ks_java_01_lms.module;

import com.example.hcm25_cpl_ks_java_01_lms.modulegroup.ModuleGroup;
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

public class ModuleExcelImporter {
    public static List<Module> importModules(InputStream inputStream) throws IOException {
        List<Module> modules = new ArrayList<>();
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

            Module module = new Module();
            int cellIdx = 0;
            while (cellsInRow.hasNext()) {
                Cell currentCell = cellsInRow.next();
                switch (cellIdx) {
                    case 0:
                        break; // skip Id because jpa not accept entity have id when create
                    case 1:
                        module.setName(currentCell.getStringCellValue());
                        break;
                    case 2:
                        module.setUrl(currentCell.getStringCellValue());
                        break;
                    case 3:
                        module.setIcon(currentCell.getStringCellValue());
                        break;
                    case 4:
                        ModuleGroup moduleGroup = new ModuleGroup();
                        moduleGroup.setName(currentCell.getStringCellValue());
                        module.setModuleGroup(moduleGroup);
                        break;
                    default:
                        break;
                }
                cellIdx++;
            }

            modules.add(module);
        }

        workbook.close();
        return modules;
    }
}
