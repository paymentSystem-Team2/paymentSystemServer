package sparta.paymentsystemserver.domain.product.dto;

import org.springframework.web.multipart.MultipartFile;

public record UploadProductImage(
        MultipartFile file,
        Integer order,
        Boolean isThumbnail
) {

}
