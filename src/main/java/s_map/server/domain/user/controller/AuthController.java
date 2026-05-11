package s_map.server.domain.user.controller;

import s_map.server.domain.user.dto.req.LoginRequest;
import s_map.server.domain.user.dto.res.LoginResponse;
import s_map.server.domain.user.service.AuthService;
import s_map.server.global.common.BaseResponse;
import s_map.server.domain.user.dto.res.AuthMeResponse;
import s_map.server.domain.user.dto.req.LogoutRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return BaseResponse.success(authService.login(request));
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public BaseResponse<AuthMeResponse> getMyInfo(Authentication authentication) {
        return BaseResponse.success(authService.getMyInfo(authentication.getName()));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public BaseResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return BaseResponse.success(null);
    }
}
