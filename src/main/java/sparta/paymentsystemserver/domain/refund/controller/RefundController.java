package sparta.paymentsystemserver.domain.refund.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.auth.dto.LoginUserData;
import sparta.paymentsystemserver.domain.payment.dto.PaymentResultResponse;
import sparta.paymentsystemserver.domain.refund.dto.RefundRequest;
import sparta.paymentsystemserver.domain.refund.service.RefundService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class RefundController {

    private final RefundService refundService;

    // 전액 환불 요청 API, 클라이언트는 환불 요청만 보내고 실제 포트원 결제 취소와 내부 환불 상태 반영은 서버가 책임진다
    @PostMapping("/{paymentId}/cancel")
    public PaymentResultResponse cancelPayment(
            @PathVariable String paymentId,
            @RequestBody RefundRequest request,
            @AuthenticationPrincipal LoginUserData loginUserData
    ) {
        return refundService.cancelPayment(paymentId, loginUserData.userId(), request.reason());
    }

}
