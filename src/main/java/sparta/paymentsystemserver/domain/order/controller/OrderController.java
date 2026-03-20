package sparta.paymentsystemserver.domain.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.auth.dto.LoginUserData;
import sparta.paymentsystemserver.domain.order.dto.GetOrderDetailResponse;
import sparta.paymentsystemserver.domain.order.dto.GetOrderListResponse;
import sparta.paymentsystemserver.domain.order.service.OrderService;
import sparta.paymentsystemserver.domain.order.dto.CreateOrderRequest;
import sparta.paymentsystemserver.domain.order.dto.CreateOrderResponse;

import java.util.List;

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

    // 주문 목록 조회
    @GetMapping
    public ResponseEntity<List<GetOrderListResponse>> getMyOrders(
            @AuthenticationPrincipal LoginUserData loginUserData
    ) {
        return ResponseEntity.ok(orderService.getMyOrders(loginUserData.userId()));
    }

    // 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<GetOrderDetailResponse> getMyOrderDetail(
            @AuthenticationPrincipal LoginUserData loginUserData,
            @PathVariable String orderId
    ) {
        return ResponseEntity.ok(orderService.getMyOrderDetail(loginUserData.userId(), orderId));
    }

}