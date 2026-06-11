package s_map.server.domain.report.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "보고서 PDF 다운로드 내부 응답 DTO")
public record ReportPdfDownloadResponse(
        @Schema(description = "다운로드 파일명", example = "2026년_6월_생산_보고서_1.pdf")
        String fileName,

        @Schema(description = "응답 MIME 타입", example = "application/pdf")
        String contentType,

        @Schema(description = "PDF 파일 byte 배열")
        byte[] content
) {
    @Schema(description = "PDF 파일 크기(byte)", example = "48215")
    public long contentLength() {
        return content == null ? 0 : content.length;
    }
}
