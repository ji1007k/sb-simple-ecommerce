package com.jikim.ecommerce.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.annotation.PostConstruct;

@Configuration
@ConfigurationProperties(prefix = "excel.security")
@Slf4j
public class ExcelSecurityConfig {
    
    @Value("${excel.max-file-size:100MB}")
    private DataSize maxFileSize;
    
    @Value("${excel.max-rows:1000000}")
    private int maxRows;
    
    @Value("${excel.max-columns:1000}")
    private int maxColumns;
    
    @Value("${excel.enable-strict-validation:true}")
    private boolean enableStrictValidation;
    
    @PostConstruct
    public void init() {
        log.info("🔒 Excel Security Configuration:");
        log.info("  - Max File Size: {}", maxFileSize);
        log.info("  - Max Rows: {}", maxRows);
        log.info("  - Max Columns: {}", maxColumns);
        log.info("  - Strict Validation: {}", enableStrictValidation);
        
        // POI 보안 설정 적용
        configurePoiSecurity();
    }
    
    private void configurePoiSecurity() {
        try {
            // XML 파서 보안 설정
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", 
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            System.setProperty("javax.xml.parsers.SAXParserFactory", 
                "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
            
            // XXE 공격 방지 설정
            System.setProperty("javax.xml.accessExternalDTD", "");
            System.setProperty("javax.xml.accessExternalSchema", "");
            System.setProperty("javax.xml.accessExternalStylesheet", "");
            
            log.info("✅ POI Security settings applied");
        } catch (Exception e) {
            log.warn("⚠️ Failed to apply some POI security settings: {}", e.getMessage());
        }
    }
    
    /**
     * 파일 유효성 검사
     */
    public void validateFile(String filename, long fileSize) {
        if (fileSize > maxFileSize.toBytes()) {
            throw new SecurityException(
                String.format("File size (%d bytes) exceeds maximum allowed size (%s)", 
                    fileSize, maxFileSize));
        }
        
        if (!isValidExcelFile(filename)) {
            throw new SecurityException("Invalid file type. Only Excel files are allowed.");
        }
    }
    
    /**
     * Excel 파일 형식 검증
     */
    public boolean isValidExcelFile(String filename) {
        if (filename == null) return false;
        
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".xlsx") || 
               lowerFilename.endsWith(".xls") ||
               lowerFilename.endsWith(".xlsm");
    }
    
    /**
     * 콘텐츠 타입 검증
     */
    public boolean isValidContentType(String contentType) {
        if (contentType == null) return false;
        
        return contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
               contentType.equals("application/vnd.ms-excel") ||
               contentType.equals("application/vnd.ms-excel.sheet.macroEnabled.12");
    }
    
    // Getters
    public DataSize getMaxFileSize() { return maxFileSize; }
    public int getMaxRows() { return maxRows; }
    public int getMaxColumns() { return maxColumns; }
    public boolean isEnableStrictValidation() { return enableStrictValidation; }
}
