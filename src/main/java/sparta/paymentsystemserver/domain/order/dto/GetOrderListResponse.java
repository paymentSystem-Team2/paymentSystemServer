package sparta.paymentsystemserver.domain.order.dto;

import java.time.LocalDateTime;

public record GetOrderListResponse(
        String orderId,
        String orderNumber,
        Long totalAmount,
        Long usedPoints,
        Long pointDiscountAmount,
        String status,
        LocalDateTime orderedAt
) {
}
