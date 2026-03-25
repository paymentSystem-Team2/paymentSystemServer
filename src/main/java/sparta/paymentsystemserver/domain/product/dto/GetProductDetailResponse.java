package sparta.paymentsystemserver.domain.product.dto;

import java.util.List;

public record GetProductDetailResponse(
        String id,
        String name,
        Long price,
        Long stock,
        String description,
        String status,
        List<String> productImages
) {
}
