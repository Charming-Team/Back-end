package s_map.server.domain.user.controller;

import s_map.server.domain.user.dto.req.AdminUserCreateRequest;
import s_map.server.domain.user.dto.res.AdminUserCreateResponse;
import s_map.server.domain.user.service.AdminAuthService;
import s_map.server.global.common.BaseResponse;
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

    @Operation(summary = "사용자 생성")
    @PostMapping("/users")
    public BaseResponse<AdminUserCreateResponse> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        return BaseResponse.success(adminAuthService.createUser(request));
    }
}
