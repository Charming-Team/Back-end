package s_map.server.domain.user.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuthLogoutRequest {

    @NotBlank(message = "Refresh Token은 필수입니다.")
    private String refreshToken;
}