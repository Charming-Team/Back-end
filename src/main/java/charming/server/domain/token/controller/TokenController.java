package charming.server.domain.token.controller;

import charming.server.domain.token.dto.req.TokenRefreshRequest;
import charming.server.domain.token.dto.res.TokenRefreshResponse;
import charming.server.domain.token.service.TokenService;
import charming.server.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Token", description = "토큰 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token")
public class TokenController {

    private final TokenService tokenService;

    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    public BaseResponse<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return BaseResponse.success(tokenService.refreshToken(request));
    }
}
