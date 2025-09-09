package com.jikim.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadProgress {
    private String requestId;
    private String status;
    private long totalCount;
    private long processedCount;
    private int progressPercentage;
    private String message;
    private String downloadUrl;
    
    public static DownloadProgress queued(String requestId) {
        return DownloadProgress.builder()
                .requestId(requestId)
                .status("QUEUED")
                .progressPercentage(0)
                .message("다운로드 요청이 대기열에 추가되었습니다.")
                .build();
    }
    
    public static DownloadProgress processing(String requestId, long totalCount, long processedCount) {
        int percentage = totalCount > 0 ? (int) ((processedCount * 100) / totalCount) : 0;
        return DownloadProgress.builder()
                .requestId(requestId)
                .status("PROCESSING")
                .totalCount(totalCount)
                .processedCount(processedCount)
                .progressPercentage(percentage)
                .message(String.format("처리 중... (%d/%d)", processedCount, totalCount))
                .build();
    }
    
    public static DownloadProgress completed(String requestId, String downloadUrl) {
        return DownloadProgress.builder()
                .requestId(requestId)
                .status("COMPLETED")
                .progressPercentage(100)
                .message("다운로드가 완료되었습니다.")
                .downloadUrl(downloadUrl)
                .build();
    }
    
    public static DownloadProgress failed(String requestId, String errorMessage) {
        return DownloadProgress.builder()
                .requestId(requestId)
                .status("FAILED")
                .message("다운로드 실패: " + errorMessage)
                .build();
    }
}
