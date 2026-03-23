package sparta.paymentsystemserver.domain.point.dto;

import sparta.paymentsystemserver.domain.point.entity.PointTransaction;
import sparta.paymentsystemserver.domain.point.entity.PointTransactionType;

import java.time.LocalDateTime;

public record PointHistoryResponse (
        String pointTransactionId,
        PointTransactionType type,
        Long points,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {

    public static PointHistoryResponse from(PointTransaction pt) {
        return new PointHistoryResponse(
                pt.getPointTransactionId(),
                pt.getType(),
                pt.getPoints(),
                pt.getExpiresAt(),
                pt.getCreatedAt()
        );
    }
}
