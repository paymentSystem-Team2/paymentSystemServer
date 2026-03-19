package sparta.paymentsystemserver.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// 상품 묶어서 요청
public record CreateOrderRequest(

        @Valid
        @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
        List<CreateOrderItemRequest> items,

        @NotNull(message = "총 주문 금액은 필수입니다.")
        Long totalAmount

) { }
