package sparta.paymentsystemserver.domain.product.dto;

public record ProductResponse(
        // 프론트 > product.id
        String id,
        String name,
        Long price,
        Long stock,
        String thumbnailImage ){}
