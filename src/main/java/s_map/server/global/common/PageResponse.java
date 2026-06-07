package s_map.server.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "페이지 응답")
public record PageResponse<T>(
        @Schema(description = "현재 페이지 데이터 목록")
        List<T> content,

        @Schema(description = "현재 페이지 번호, 0부터 시작", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "10")
        int size,

        @Schema(description = "현재 페이지 데이터 수", example = "10")
        int numberOfElements,

        @Schema(description = "전체 데이터 수", example = "125")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "13")
        int totalPages,

        @Schema(description = "첫 페이지 여부", example = "true")
        boolean first,

        @Schema(description = "마지막 페이지 여부", example = "false")
        boolean last,

        @Schema(description = "데이터 없음 여부", example = "false")
        boolean empty
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}
