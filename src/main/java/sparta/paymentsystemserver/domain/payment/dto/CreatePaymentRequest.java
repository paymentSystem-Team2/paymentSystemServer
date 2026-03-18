package sparta.paymentsystemserver.domain.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// 결제 시도 생성 요청 dto
public record CreatePaymentRequest(
        // 어떤 주문을 결제할지 가리키는 공개 주문 Id
        @NotBlank(message = "orderId는 필수입니다.")
        String orderId,

        // 프론트가 계산해서 전달한 주문 총액, 서버가 주문 스냅샷이랑 다시 비교
        @Min(value = 0, message = "totalAmount는 0 이상이어야 합니다.")
        long totalAmount,

        // 이번 결제에 사용하려는 포인트 금액
        @Min(value = 0, message = "pointsToUser는 0 이상이어야 합니다.")
        long pointsToUse
) {
}
