package s_map.server.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.chat.dto.req.ChatAnswerRequest;
import s_map.server.domain.chat.dto.res.ChatAnswerResponse;
import s_map.server.domain.chat.service.ChatService;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.security.AuthUser;

@Tag(name = "Chat", description = "사용자 챗봇 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "챗봇 답변 요청",
            description = "JWT 인증 사용자 컨텍스트를 포함해 FastAPI AI 챗봇 답변 API를 호출합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "챗봇 답변 요청 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "502", description = "챗봇 서버 연결 또는 응답 처리 실패"),
            @ApiResponse(responseCode = "503", description = "챗봇 서버 사용 불가"),
            @ApiResponse(responseCode = "504", description = "챗봇 응답 시간 초과")
    })
    @PostMapping("/answer")
    public BaseResponse<ChatAnswerResponse> answer(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ChatAnswerRequest request
    ) {
        return BaseResponse.success(chatService.answer(authUser, request));
    }
}
