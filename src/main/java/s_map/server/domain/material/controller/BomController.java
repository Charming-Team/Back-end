package s_map.server.domain.material.controller;

import s_map.server.domain.material.dto.req.BomCreateRequest;
import s_map.server.domain.material.dto.req.BomUpdateRequest;
import s_map.server.domain.material.dto.res.BomResponse;
import s_map.server.domain.material.service.BomService;
import s_map.server.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "BOM", description = "제품별 자재 소요량 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/materials/boms")
public class BomController {

    private final BomService bomService;

    @Operation(
            summary = "BOM 등록",
            description = "제품 1단위 생산에 필요한 자재 소요량과 손실률을 등록합니다. 손실률은 퍼센트 단위이며 5.00은 5%를 의미합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "BOM 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "자재 없음"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 제품-자재 BOM"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping
    public BaseResponse<BomResponse> createBom(
            @Valid @RequestBody BomCreateRequest request
    ) {
        return BaseResponse.success(bomService.createBom(request));
    }

    @Operation(
            summary = "BOM 목록 조회",
            description = "BOM 목록을 페이지 단위로 bomId 내림차순 조회합니다. 각 BOM에는 연결된 자재 기본 정보가 함께 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "BOM 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public BaseResponse<Page<BomResponse>> getBoms(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기, 최대 100", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return BaseResponse.success(bomService.getBoms(page, size));
    }

    @Operation(
            summary = "제품별 BOM 목록 조회",
            description = "제품 ID를 기준으로 해당 제품에 등록된 BOM 목록을 bomId 내림차순 조회합니다. 등록된 BOM이 없으면 빈 목록을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제품별 BOM 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/products/{productId}")
    public BaseResponse<List<BomResponse>> getBomsByProductId(
            @Parameter(description = "제품 ID", example = "1")
            @PathVariable Long productId
    ) {
        return BaseResponse.success(bomService.getBomsByProductId(productId));
    }

    @Operation(
            summary = "BOM 수정",
            description = "BOM의 제품 1단위당 자재 소요량, 단위, 손실률을 수정합니다. 단위가 비어 있으면 연결된 자재의 기본 단위를 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "BOM 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "BOM 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PutMapping("/{bomId}")
    public BaseResponse<BomResponse> updateBom(
            @Parameter(description = "BOM ID", example = "1")
            @PathVariable Long bomId,
            @Valid @RequestBody BomUpdateRequest request
    ) {
        return BaseResponse.success(bomService.updateBom(bomId, request));
    }
}
