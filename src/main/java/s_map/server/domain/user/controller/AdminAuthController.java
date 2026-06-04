package s_map.server.domain.user.controller;

import s_map.server.domain.user.dto.req.AdminUserCreateRequest;
import s_map.server.domain.user.dto.res.AdminDashboardResponse;
import s_map.server.domain.user.dto.res.AdminUserCreateResponse;
import s_map.server.domain.user.service.AdminAuthService;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.security.AuthUser;
import s_map.server.domain.user.dto.res.AdminUserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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

    @Operation(summary = "사용자 목록 조회")
    @GetMapping("/users")
    public BaseResponse<Page<AdminUserResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(adminAuthService.getUsers(page, size, keyword));
    }

    @Operation(summary = "관리자 대시보드 사용자 현황 조회")
    @GetMapping("/dashboard")
    public BaseResponse<AdminDashboardResponse> getDashboard() {
        return BaseResponse.success(adminAuthService.getDashboard());
    }

    @Operation(summary = "사용자 삭제")
    @DeleteMapping("/users/{userId}")
    public BaseResponse<Void> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        adminAuthService.deleteUser(userId, authUser.id());
        return BaseResponse.success(null);
    }
}
