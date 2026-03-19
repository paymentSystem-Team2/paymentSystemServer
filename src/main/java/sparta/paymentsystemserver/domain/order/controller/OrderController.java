package sparta.paymentsystemserver.domain.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sparta.paymentsystemserver.domain.auth.dto.LoginUserData;
import sparta.paymentsystemserver.domain.order.service.OrderService;
import sparta.paymentsystemserver.domain.order.dto.CreateOrderRequest;
import sparta.paymentsystemserver.domain.order.dto.CreateOrderResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    // 주문 비지니스 로직 처리 서비스
    private final OrderService orderService;

    @PostMapping
    public CreateOrderResponse createOrder(
            // 로그인한 사용자
            @AuthenticationPrincipal LoginUserData loginUserData,
            // 요청 바디 검증
            @Valid @RequestBody CreateOrderRequest request
    ){
        // 주문 생성 서비스 호출
        return orderService.createOrder(loginUserData.userId(), request);
    }
}