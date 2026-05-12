package s_map.server.domain.material.controller;

import s_map.server.domain.material.dto.req.MaterialCreateRequest;
import s_map.server.domain.material.dto.req.MaterialUpdateRequest;
import s_map.server.domain.material.dto.res.MaterialDetailResponse;
import s_map.server.domain.material.dto.res.MaterialResponse;
import s_map.server.domain.material.dto.res.MaterialUsageResponse;
import s_map.server.domain.material.service.MaterialService;
import s_map.server.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Material", description = "자재 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;

    @Operation(summary = "자재 등록")
    @PostMapping
    public BaseResponse<MaterialDetailResponse> createMaterial(
            @Valid @RequestBody MaterialCreateRequest request
    ) {
        return BaseResponse.success(materialService.createMaterial(request));
    }

    @Operation(summary = "자재 목록 조회")
    @GetMapping
    public BaseResponse<List<MaterialResponse>> getMaterials() {
        return BaseResponse.success(materialService.getMaterials());
    }

    @Operation(summary = "자재 상세 조회")
    @GetMapping("/{materialId}")
    public BaseResponse<MaterialDetailResponse> getMaterial(
            @PathVariable Long materialId
    ) {
        return BaseResponse.success(materialService.getMaterial(materialId));
    }

    @Operation(summary = "자재 정보 수정")
    @PatchMapping("/{materialId}")
    public BaseResponse<MaterialDetailResponse> updateMaterial(
            @PathVariable Long materialId,
            @Valid @RequestBody MaterialUpdateRequest request
    ) {
        return BaseResponse.success(materialService.updateMaterial(materialId, request));
    }

    @Operation(summary = "자재 사용량 조회")
    @GetMapping("/{materialId}/usage")
    public BaseResponse<MaterialUsageResponse> getMaterialUsage(
            @PathVariable Long materialId
    ) {
        return BaseResponse.success(materialService.getMaterialUsage(materialId));
    }
}