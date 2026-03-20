package sparta.paymentsystemserver.domain.order.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GetOrderDetailResponse(
        String orderId,
        String orderNumber,
        Long totalAmount,
        Long usedPoints,
        Long pointDiscountAmount,
        String status,
        LocalDateTime orderedAt,
        List<GetOrderItemResponse> items
) {
}