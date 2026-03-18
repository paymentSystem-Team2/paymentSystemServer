package sparta.paymentsystemserver.domain.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.auth.dto.LoginUserData;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentRequest;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentResponse;
import sparta.paymentsystemserver.domain.payment.dto.PaymentResultResponse;
import sparta.paymentsystemserver.domain.payment.service.PaymentService;

// 결제를 생성하고 확정, 환불을 하는 API입니다
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 시도 생성 API JWT에서 꺼낸 현재 로그인 사용자 정보 사용
    @PostMapping
    public CreatePaymentResponse createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal LoginUserData loginUserData
    ) {
        return paymentService.createPayment(request, loginUserData.userId());
    }

    // 결제 확정 API 프론트에서 PortOne 결제창 완료 후 paymentId를 들고 호출
    @PostMapping("/{paymentId}/confirm")
    public PaymentResultResponse confirmPayment(
            @PathVariable String paymentId,
            @AuthenticationPrincipal LoginUserData loginUserData
    ) {
        return paymentService.confirmPayment(paymentId, loginUserData.userId());
    }

}
