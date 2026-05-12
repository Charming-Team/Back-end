package s_map.server.domain.material.controller;

import s_map.server.domain.material.dto.req.BomCreateRequest;
import s_map.server.domain.material.dto.req.BomUpdateRequest;
import s_map.server.domain.material.dto.res.BomResponse;
import s_map.server.domain.material.service.BomService;
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

@Tag(name = "BOM", description = "제품별 자재 소요량 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/materials/boms")
public class BomController {

    private final BomService bomService;

    @Operation(summary = "BOM 등록")
    @PostMapping
    public BaseResponse<BomResponse> createBom(
            @Valid @RequestBody BomCreateRequest request
    ) {
        return BaseResponse.success(bomService.createBom(request));
    }

    @Operation(summary = "BOM 목록 조회")
    @GetMapping
    public BaseResponse<List<BomResponse>> getBoms() {
        return BaseResponse.success(bomService.getBoms());
    }

    @Operation(summary = "제품별 BOM 목록 조회")
    @GetMapping("/products/{productId}")
    public BaseResponse<List<BomResponse>> getBomsByProductId(
            @PathVariable Long productId
    ) {
        return BaseResponse.success(bomService.getBomsByProductId(productId));
    }

    @Operation(summary = "BOM 수정")
    @PatchMapping("/{bomId}")
    public BaseResponse<BomResponse> updateBom(
            @PathVariable Long bomId,
            @Valid @RequestBody BomUpdateRequest request
    ) {
        return BaseResponse.success(bomService.updateBom(bomId, request));
    }
}