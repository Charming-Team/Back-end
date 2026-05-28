package s_map.server.domain.chat.dto.req;

public record FastApiChatUserContext(
        Long userId,
        String role,
        String companyName,
        String status
) {
}
