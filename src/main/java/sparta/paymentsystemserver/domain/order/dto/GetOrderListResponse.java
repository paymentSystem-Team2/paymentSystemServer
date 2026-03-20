package sparta.paymentsystemserver.domain.order.dto;

import java.time.LocalDateTime;

public record GetOrderListResponse(
        String orderNumber,
        String orderId,
        Long totalAmount,
        Long usedPoints,
        Long finalAmount,
        Long earnedPoints,
        String currency,
        String status,
        LocalDateTime createdAt
) {
}
