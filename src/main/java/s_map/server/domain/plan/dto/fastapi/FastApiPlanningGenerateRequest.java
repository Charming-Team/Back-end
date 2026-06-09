package s_map.server.domain.plan.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Schema(description = "FastAPI 생산계획 조정 요청")
public class FastApiPlanningGenerateRequest {

    @Schema(description = "재계획 시작 시각", example = "2026-05-01 09:00:00.000 +0900")
    @NotBlank(message = "재계획 시작 시각은 필수입니다.")
    @JsonProperty("planning_start")
    private String planningStart;

    @Schema(description = "재계획 종료 시각", example = "2026-06-09 08:59:00.000 +0900")
    @NotBlank(message = "재계획 종료 시각은 필수입니다.")
    @JsonProperty("planning_end")
    private String planningEnd;

    @Schema(description = "사용자가 수정해서 고정할 기존 생산계획 목록. 없으면 빈 배열")
    @NotNull(message = "고정할 기존 생산계획 목록은 필수입니다. 없으면 빈 배열을 전달해주세요.")
    @Valid
    @JsonProperty("edit_orders")
    private List<PlanningEditOrder> editOrders;

    @Schema(description = "새로 추가하거나 이동 가능한 주문 목록. 없으면 빈 배열")
    @NotNull(message = "추가 주문 목록은 필수입니다. 없으면 빈 배열을 전달해주세요.")
    @Valid
    @JsonProperty("add_orders")
    private List<PlanningAddOrder> addOrders;

    public static FastApiPlanningGenerateRequest of(
            String planningStart,
            String planningEnd,
            List<PlanningEditOrder> editOrders,
            List<PlanningAddOrder> addOrders
    ) {
        FastApiPlanningGenerateRequest request = new FastApiPlanningGenerateRequest();
        request.planningStart = planningStart;
        request.planningEnd = planningEnd;
        request.editOrders = editOrders;
        request.addOrders = addOrders;
        return request;
    }

    @Getter
    @Schema(description = "FastAPI 생산계획 조정 대상 기존 주문")
    public static class PlanningEditOrder {

        @Schema(description = "기존 생산계획 row ID 또는 schedule_id", example = "399")
        @NotNull(message = "주문 ID는 필수입니다.")
        @JsonProperty("order_id")
        private Long orderId;

        @Schema(description = "제품 ID", example = "10")
        @NotNull(message = "제품 ID는 필수입니다.")
        @JsonProperty("product_id")
        private Long productId;

        @Schema(description = "계획 대상 수량", example = "16800")
        @NotNull(message = "주문 수량은 필수입니다.")
        @JsonProperty("order_quantity")
        private Integer orderQuantity;

        @Schema(description = "납기 시각", example = "2026-05-22 08:59:59.000 +0900")
        @NotBlank(message = "납기 시각은 필수입니다.")
        @JsonProperty("due_date")
        private String dueDate;

        @Schema(description = "수주 금액", example = "30752426.00")
        @NotNull(message = "수주 금액은 필수입니다.")
        @JsonProperty("contract_amount")
        private BigDecimal contractAmount;

        @Schema(description = "납기 지연 패널티 금액", example = "833160.00")
        @NotNull(message = "납기 지연 패널티 금액은 필수입니다.")
        @JsonProperty("late_penalty_amount")
        private BigDecimal latePenaltyAmount;

        @Schema(description = "주문/계획 상태", example = "DELAYED")
        @NotBlank(message = "주문 상태는 필수입니다.")
        @JsonProperty("order_status")
        private String orderStatus;

        @Schema(description = "사용자가 고정한 라인/시작/종료 시간")
        @NotNull(message = "기존 계획 수정 요청에는 고정 계획 정보가 필수입니다.")
        @Valid
        @JsonProperty("locked_plan")
        private LockedPlan lockedPlan;

        public static PlanningEditOrder of(
                Long orderId,
                Long productId,
                Integer orderQuantity,
                String dueDate,
                BigDecimal contractAmount,
                BigDecimal latePenaltyAmount,
                String orderStatus,
                LockedPlan lockedPlan
        ) {
            PlanningEditOrder order = new PlanningEditOrder();
            order.orderId = orderId;
            order.productId = productId;
            order.orderQuantity = orderQuantity;
            order.dueDate = dueDate;
            order.contractAmount = contractAmount;
            order.latePenaltyAmount = latePenaltyAmount;
            order.orderStatus = orderStatus;
            order.lockedPlan = lockedPlan;
            return order;
        }
    }

    @Getter
    @Schema(description = "FastAPI 생산계획 조정 대상 신규 또는 이동 가능 주문")
    public static class PlanningAddOrder {

        @Schema(description = "신규 주문 ID 또는 DB 기존 계획 row ID", example = "900000001")
        @NotNull(message = "주문 ID는 필수입니다.")
        @JsonProperty("order_id")
        private Long orderId;

        @Schema(description = "제품 ID", example = "10")
        @NotNull(message = "제품 ID는 필수입니다.")
        @JsonProperty("product_id")
        private Long productId;

        @Schema(description = "계획 대상 수량", example = "1200")
        @NotNull(message = "주문 수량은 필수입니다.")
        @JsonProperty("order_quantity")
        private Integer orderQuantity;

        @Schema(description = "납기 시각", example = "2026-05-21 09:00:00.000 +0900")
        @NotBlank(message = "납기 시각은 필수입니다.")
        @JsonProperty("due_date")
        private String dueDate;

        @Schema(description = "수주 금액", example = "1500000.00")
        @NotNull(message = "수주 금액은 필수입니다.")
        @JsonProperty("contract_amount")
        private BigDecimal contractAmount;

        @Schema(description = "납기 지연 패널티 금액", example = "120000.00")
        @NotNull(message = "납기 지연 패널티 금액은 필수입니다.")
        @JsonProperty("late_penalty_amount")
        private BigDecimal latePenaltyAmount;

        @Schema(description = "주문/계획 상태", example = "SCHEDULED")
        @NotBlank(message = "주문 상태는 필수입니다.")
        @JsonProperty("order_status")
        private String orderStatus;

        public static PlanningAddOrder of(
                Long orderId,
                Long productId,
                Integer orderQuantity,
                String dueDate,
                BigDecimal contractAmount,
                BigDecimal latePenaltyAmount,
                String orderStatus
        ) {
            PlanningAddOrder order = new PlanningAddOrder();
            order.orderId = orderId;
            order.productId = productId;
            order.orderQuantity = orderQuantity;
            order.dueDate = dueDate;
            order.contractAmount = contractAmount;
            order.latePenaltyAmount = latePenaltyAmount;
            order.orderStatus = orderStatus;
            return order;
        }
    }

    @Getter
    @Schema(description = "사용자가 고정한 생산계획")
    public static class LockedPlan {

        @Schema(description = "고정할 생산 라인 ID", example = "6")
        @NotNull(message = "고정 라인 ID는 필수입니다.")
        @JsonProperty("line_id")
        private Long lineId;

        @Schema(description = "고정된 생산 시작 시각", example = "2026-06-02 00:57:31.000 +0900")
        @NotBlank(message = "고정 시작 시각은 필수입니다.")
        @JsonProperty("planned_start_at")
        private String plannedStartAt;

        @Schema(description = "고정된 생산 종료 시각", example = "2026-06-03 02:33:31.000 +0900")
        @NotBlank(message = "고정 종료 시각은 필수입니다.")
        @JsonProperty("planned_end_at")
        private String plannedEndAt;

        public static LockedPlan of(
                Long lineId,
                String plannedStartAt,
                String plannedEndAt
        ) {
            LockedPlan lockedPlan = new LockedPlan();
            lockedPlan.lineId = lineId;
            lockedPlan.plannedStartAt = plannedStartAt;
            lockedPlan.plannedEndAt = plannedEndAt;
            return lockedPlan;
        }
    }
}
