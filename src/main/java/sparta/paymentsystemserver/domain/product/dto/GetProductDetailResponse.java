package sparta.paymentsystemserver.domain.product.dto;

import java.util.Map;

public record GetProductDetailResponse(
        String id,
        String name,
        Long price,
        Long stock,
        String description,
        String status,
        Map<String,Integer> productImages
) {
}
