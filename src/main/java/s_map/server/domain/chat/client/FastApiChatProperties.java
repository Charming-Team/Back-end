package s_map.server.domain.chat.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.fastapi")
public class FastApiChatProperties {

    private String baseUrl = "http://fastapi-service:8000";
    private String chatAnswerPath = "/ai/api/v1/chat/answer";
    private String chatAnswerInternalToken = "";
    private long timeoutSeconds = 60;
}
