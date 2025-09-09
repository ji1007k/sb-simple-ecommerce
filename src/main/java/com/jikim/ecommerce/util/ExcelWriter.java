package com.jikim.ecommerce.util;

import com.jikim.ecommerce.entity.SampleData;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class ExcelWriter {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 스트리밍 방식으로 엑셀 파일 생성
     * SXSSFWorkbook을 사용하여 메모리 사용량 최소화
     */
    public static void writeExcelStreaming(String filePath, 
                                         List<SampleData> dataList,
                                         Consumer<Integer> progressCallback) throws IOException {
        
        // 메모리에 100개 행만 유지하고 나머지는 임시 파일로 처리
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Sample Data");
            
            // 컬럼 너비를 미리 설정 (autoSizeColumn 대신)
            sheet.setColumnWidth(0, 3000);   // ID
            sheet.setColumnWidth(1, 6000);   // 이름
            sheet.setColumnWidth(2, 8000);   // 설명
            sheet.setColumnWidth(3, 4000);   // 가격
            sheet.setColumnWidth(4, 4000);   // 카테고리
            sheet.setColumnWidth(5, 5000);   // 생성일시
            
            // 헤더 스타일
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // 헤더 생성
            createHeader(sheet, headerStyle);
            
            // 데이터 행 생성
            int rowIndex = 1;
            for (int i = 0; i < dataList.size(); i++) {
                SampleData data = dataList.get(i);
                Row row = sheet.createRow(rowIndex++);
                
                createCell(row, 0, data.getId(), dataStyle);
                createCell(row, 1, data.getName(), dataStyle);
                createCell(row, 2, data.getDescription(), dataStyle);
                createCell(row, 3, data.getPrice(), dataStyle);
                createCell(row, 4, data.getCategory(), dataStyle);
                createCell(row, 5, data.getCreatedAt().format(DATE_FORMATTER), dataStyle);
                
                // 진행률 콜백 (100건마다 호출)
                if (i > 0 && i % 100 == 0) {
                    progressCallback.accept(i);
                }
            }
            
            // 파일 저장
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            
            // 임시 파일 정리
            workbook.dispose();
            
            log.info("Excel file created successfully: {}", filePath);
        }
    }
    
    private static void createHeader(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        
        String[] headers = {"ID", "이름", "설명", "가격", "카테고리", "생성일시"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }
    
    private static void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
        
        cell.setCellStyle(style);
    }
    
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }
    
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }
}
