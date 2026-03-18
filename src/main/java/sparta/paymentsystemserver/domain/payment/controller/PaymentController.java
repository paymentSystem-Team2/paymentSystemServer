package sparta.paymentsystemserver.domain.payment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentRequest;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentResponse;
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
            @AuthenticationPrincipal Long userId
    ) {
        return paymentService.createPayment(request, userId);
    }

}
