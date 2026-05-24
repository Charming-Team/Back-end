package s_map.server.domain.chat.dto.req;

import java.time.OffsetDateTime;

public record FastApiChatAnswerRequest(
        Long sessionId,
        Long messageId,
        FastApiChatUserContext user,
        String question,
        OffsetDateTime requestedAt
) {
}
