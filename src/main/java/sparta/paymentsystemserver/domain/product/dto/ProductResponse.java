package sparta.paymentsystemserver.domain.product.dto;

import java.net.URL;

public record ProductResponse(
        // 프론트 > product.id
        String id,
        String name,
        Long price,
        Long stock,
        URL thumbnailImage ){}
