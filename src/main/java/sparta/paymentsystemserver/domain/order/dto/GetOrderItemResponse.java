package sparta.paymentsystemserver.domain.order.dto;

public record GetOrderItemResponse (
        String productId,
        String productName,
        Long productPrice,
        Long quantity
){
}
