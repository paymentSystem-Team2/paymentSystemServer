package sparta.paymentsystemserver.domain.subscription.dto;

import java.time.LocalDateTime;

// 즉시 청구 요청 dto
public record CreateBillingRequest(
        LocalDateTime periodStart,
        LocalDateTime periodEnd
) {
}
