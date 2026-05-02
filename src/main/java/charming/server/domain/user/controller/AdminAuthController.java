package charming.server.domain.user.controller;

import charming.server.domain.user.dto.req.AdminLoginRequest;
import charming.server.domain.user.dto.req.AdminUserCreateRequest;
import charming.server.domain.user.dto.res.AdminLoginResponse;
import charming.server.domain.user.dto.res.AdminUserCreateResponse;
import charming.server.domain.user.service.AdminAuthService;
import charming.server.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "관리자 로그인")
    @PostMapping("/login")
    public BaseResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return BaseResponse.success(adminAuthService.login(request));
    }

    @Operation(summary = "사용자 생성")
    @PostMapping("/users")
    public BaseResponse<AdminUserCreateResponse> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        return BaseResponse.success(adminAuthService.createUser(request));
    }
}
