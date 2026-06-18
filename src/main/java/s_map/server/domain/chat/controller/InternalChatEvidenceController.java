package s_map.server.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.chat.dto.req.ChatEvidenceLookupRequest;
import s_map.server.domain.chat.dto.res.ChatEvidenceLookupResponse;
import s_map.server.domain.chat.service.ChatEvidenceService;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.security.InternalApiTokenValidator;

@Tag(name = "Internal Chat Evidence", description = "FastAPI 챗봇 내부 연동용 RDB Evidence API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/chat")
public class InternalChatEvidenceController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final ChatEvidenceService chatEvidenceService;
    private final InternalApiTokenValidator internalApiTokenValidator;

    @Operation(
            summary = "챗봇 RDB Evidence 조회",
            description = "FastAPI AI Agent가 질문 의도와 사용자 컨텍스트를 전달하면 Spring RDB 업무 데이터를 Evidence 형태로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "RDB Evidence 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "내부 API 토큰 누락 또는 불일치"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/evidence")
    public BaseResponse<ChatEvidenceLookupResponse> lookupEvidence(
            @Parameter(description = "FastAPI 내부 호출 토큰", required = true)
            @RequestHeader(value = INTERNAL_TOKEN_HEADER, required = false) String internalToken,
            @Parameter(description = "챗봇 Evidence 조회 요청")
            @Valid @RequestBody ChatEvidenceLookupRequest request
    ) {
        internalApiTokenValidator.validateEvidenceToken(internalToken);
        return BaseResponse.success(chatEvidenceService.lookup(request));
    }
}
