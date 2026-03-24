package sparta.paymentsystemserver.domain.order.dto;

public record updateOrderStatusResponse(
        boolean success,
        String orderId,
        String status
) {
}
