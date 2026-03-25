package sparta.paymentsystemserver.domain.product.dto;

import java.util.List;

public record UploadProductImageRequest(
        String productId,
        List<UploadProductImage> uploadProductImages
) {
}
