package com.jikim.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadRequest {
    private String requestId;
    private String fileName;
    private DownloadType downloadType;
    private String sessionId;
    
    public enum DownloadType {
        PAGING,    // 기존 방식: 페이징으로 1000건씩
        STREAMING  // 개선 방식: 스트리밍으로 메모리 효율적 처리
    }
}
