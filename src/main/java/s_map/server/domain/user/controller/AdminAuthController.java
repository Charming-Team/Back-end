package s_map.server.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.user.dto.req.AdminUserCreateRequest;
import s_map.server.domain.user.dto.res.AdminDashboardResponse;
import s_map.server.domain.user.dto.res.AdminUserCreateResponse;
import s_map.server.domain.user.dto.res.AdminUserResponse;
import s_map.server.domain.user.service.AdminAuthService;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.common.PageResponse;
import s_map.server.global.security.AuthUser;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "사용자 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/users")
    public BaseResponse<AdminUserCreateResponse> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        return BaseResponse.success(adminAuthService.createUser(request));
    }

    @Operation(summary = "사용자 목록 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/users")
    public BaseResponse<PageResponse<AdminUserResponse>> getUsers(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "이름 또는 이메일 검색어", example = "manager")
            @RequestParam(required = false) String keyword
    ) {
        return BaseResponse.success(PageResponse.from(adminAuthService.getUsers(page, size, keyword)));
    }

    @Operation(summary = "관리자 대시보드 사용자 현황 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관리자 대시보드 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/dashboard")
    public BaseResponse<AdminDashboardResponse> getDashboard() {
        return BaseResponse.success(adminAuthService.getDashboard());
    }

    @Operation(summary = "사용자 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "사용자 없음"),
            @ApiResponse(responseCode = "409", description = "삭제할 수 없는 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/users/{userId}")
    public BaseResponse<Void> deleteUser(
            @Parameter(description = "삭제할 사용자 ID", example = "10")
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        adminAuthService.deleteUser(userId, authUser.id());
        return BaseResponse.success(null);
    }
}
