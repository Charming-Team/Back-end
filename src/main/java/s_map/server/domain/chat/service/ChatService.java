package s_map.server.domain.chat.service;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.chat.client.FastApiChatClient;
import s_map.server.domain.chat.dto.req.ChatAnswerRequest;
import s_map.server.domain.chat.dto.req.FastApiChatAnswerRequest;
import s_map.server.domain.chat.dto.req.FastApiChatUserContext;
import s_map.server.domain.chat.dto.res.ChatAnswerResponse;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import s_map.server.global.security.AuthUser;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final FastApiChatClient fastApiChatClient;
    private final UserRepository userRepository;
    private final AtomicLong trackingIdSequence = new AtomicLong(System.currentTimeMillis());

    /**
     * 기능: JWT 인증 사용자 컨텍스트를 포함해 FastAPI 챗봇 답변 API를 호출한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 ID, 이메일, Role
     * - request / ChatAnswerRequest / 사용자 질문과 요청 추적용 sessionId, messageId
     *
     * Output:
     * - response / ChatAnswerResponse / FastAPI 챗봇 답변, 출처, 보안 결과, 모델 처리 결과
     */
    public ChatAnswerResponse answer(AuthUser authUser, ChatAnswerRequest request) {
        if (authUser == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        if (!user.isActive()) {
            throw new CustomException(ErrorCode.INACTIVE_ACCOUNT);
        }

        FastApiChatAnswerRequest fastApiRequest = new FastApiChatAnswerRequest(
                resolveSessionId(request),
                resolveMessageId(request),
                new FastApiChatUserContext(
                        user.getId(),
                        user.getRole().name(),
                        user.getCompanyName(),
                        user.getStatus().name()
                ),
                request.question(),
                OffsetDateTime.now()
        );

        return fastApiChatClient.requestAnswer(fastApiRequest);
    }

    /**
     * 기능: 프론트가 전달한 sessionId를 사용하거나 없으면 임시 추적용 ID를 생성한다.
     *
     * Input:
     * - request / ChatAnswerRequest / 챗봇 답변 요청 값
     *
     * Output:
     * - result / Long / FastAPI에 전달할 요청 추적용 sessionId
     */
    private Long resolveSessionId(ChatAnswerRequest request) {
        return request.sessionId() != null
                ? request.sessionId()
                : trackingIdSequence.incrementAndGet();
    }

    /**
     * 기능: 프론트가 전달한 messageId를 사용하거나 없으면 임시 추적용 ID를 생성한다.
     *
     * Input:
     * - request / ChatAnswerRequest / 챗봇 답변 요청 값
     *
     * Output:
     * - result / Long / FastAPI에 전달할 요청 추적용 messageId
     */
    private Long resolveMessageId(ChatAnswerRequest request) {
        return request.messageId() != null
                ? request.messageId()
                : trackingIdSequence.incrementAndGet();
    }
}
