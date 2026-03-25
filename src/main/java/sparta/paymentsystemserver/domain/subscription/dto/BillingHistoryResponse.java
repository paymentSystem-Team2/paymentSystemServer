package sparta.paymentsystemserver.domain.subscription.dto;

import java.util.List;

// 청구 이력 목록 응답 dto
public record BillingHistoryResponse(
        List<BillingHistoryItemResponse> billings
) {
}
