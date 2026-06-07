package s_map.server.domain.material.controller;

import s_map.server.domain.material.dto.req.MaterialCreateRequest;
import s_map.server.domain.material.dto.req.MaterialUpdateRequest;
import s_map.server.domain.material.dto.res.MaterialDetailResponse;
import s_map.server.domain.material.dto.res.MaterialResponse;
import s_map.server.domain.material.dto.res.MaterialUsageResponse;
import s_map.server.domain.material.dto.req.MaterialInventoryUpdateRequest;
import s_map.server.domain.material.service.MaterialService;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.common.PageResponse;
import s_map.server.domain.material.dto.res.MaterialShortageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.global.security.AuthUser;

import java.util.List;

@Tag(name = "Material", description = "자재 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;

    @Operation(
            summary = "자재 등록",
            description = "자재 코드, 자재명, 자재 유형, 단위, 설명을 등록합니다. 자재 코드는 중복될 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "자재 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 자재 코드"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping
    public BaseResponse<MaterialDetailResponse> createMaterial(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody MaterialCreateRequest request
    ) {
        return BaseResponse.success(materialService.createMaterial(request, authUser));
    }

    @Operation(
            summary = "자재 목록 조회",
            description = "자재 목록과 각 자재의 재고 요약 정보를 페이지 단위로 materialId 내림차순 조회합니다. 재고가 등록되지 않은 자재는 inventoryRegistered=false로 표시됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "자재 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public BaseResponse<PageResponse<MaterialResponse>> getMaterials(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기, 최대 100", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        return BaseResponse.success(PageResponse.from(materialService.getMaterials(page, size)));
    }

    @Operation(
            summary = "부족 자재 목록 조회",
            description = "생산계획별 자재 계산 결과 중 부족 또는 일부 예약 상태인 자재 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부족 자재 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/shortages")
    public BaseResponse<List<MaterialShortageResponse>> getMaterialShortages() {
        return BaseResponse.success(materialService.getMaterialShortages());
    }

    @Operation(
            summary = "자재 상세 조회",
            description = "자재 ID를 기준으로 자재 기본 정보와 재고 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "자재 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "자재 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{materialId}")
    public BaseResponse<MaterialDetailResponse> getMaterial(
            @Parameter(description = "자재 ID", example = "1")
            @PathVariable Long materialId
    ) {
        return BaseResponse.success(materialService.getMaterial(materialId));
    }

    @Operation(
            summary = "자재 정보 수정",
            description = "자재명, 자재 유형, 단위, 설명을 전체 수정합니다. 모든 필수 필드를 함께 전달해야 합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "자재 정보 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "자재 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PutMapping("/{materialId}")
    public BaseResponse<MaterialDetailResponse> updateMaterial(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "자재 ID", example = "1")
            @PathVariable Long materialId,
            @Valid @RequestBody MaterialUpdateRequest request
    ) {
        return BaseResponse.success(materialService.updateMaterial(materialId, request, authUser));
    }

    @Operation(
            summary = "자재 사용량 조회",
            description = "특정 자재의 재고 요약과 생산계획별 필요/예약/사용/부족 수량 페이지를 조회합니다. 합계는 전체 생산계획별 자재 사용량 기준으로 계산합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "자재 사용량 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "자재 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{materialId}/usage")
    public BaseResponse<MaterialUsageResponse> getMaterialUsage(
            @Parameter(description = "자재 ID", example = "1")
            @PathVariable Long materialId,
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기, 최대 100", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return BaseResponse.success(materialService.getMaterialUsage(materialId, page, size));
    }

    @Operation(
            summary = "자재 재고 등록/수정",
            description = "자재의 현재 재고, 안전 재고, 입고 예정 정보를 등록하거나 수정합니다. 예약 재고는 생산계획 예약 결과로 관리되며 이 API에서 직접 수정하지 않습니다. 현재 재고는 기존 예약 재고보다 작을 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "자재 재고 등록/수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 현재 재고가 기존 예약 재고보다 작음"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "자재 없음"),
            @ApiResponse(responseCode = "409", description = "재고 동시 수정 충돌"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PutMapping("/{materialId}/inventory")
    public BaseResponse<MaterialDetailResponse> updateMaterialInventory(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "자재 ID", example = "1")
            @PathVariable Long materialId,
            @Valid @RequestBody MaterialInventoryUpdateRequest request
    ) {
        return BaseResponse.success(materialService.updateMaterialInventory(materialId, request, authUser));
    }
}
