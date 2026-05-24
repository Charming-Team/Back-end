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

    public ChatAnswerResponse answer(AuthUser authUser, ChatAnswerRequest request) {
        if (authUser == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(authUser.id())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

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

    private Long resolveSessionId(ChatAnswerRequest request) {
        return request.sessionId() != null
                ? request.sessionId()
                : trackingIdSequence.incrementAndGet();
    }

    private Long resolveMessageId(ChatAnswerRequest request) {
        return request.messageId() != null
                ? request.messageId()
                : trackingIdSequence.incrementAndGet();
    }
}
