package sparta.paymentsystemserver.domain.product.dto;

public record GetProductDetailResponse(
        String id,
        String name,
        Long price,
        Long stock,
        String description,
        String status
) {
}
