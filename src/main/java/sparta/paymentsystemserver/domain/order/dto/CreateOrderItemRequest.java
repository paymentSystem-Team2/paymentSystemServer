package sparta.paymentsystemserver.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest (

    @NotBlank(message = "상품 ID는 필수입니다.")
    String productId,

    @NotNull(message = "수량은 필수입니다.")
    @Positive(message = "수량은 1 이상이어야 합니다.")
    Long quantity
){
}
