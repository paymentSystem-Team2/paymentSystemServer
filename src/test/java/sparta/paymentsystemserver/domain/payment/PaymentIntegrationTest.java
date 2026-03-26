package sparta.paymentsystemserver.domain.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderStatus;
import sparta.paymentsystemserver.domain.order.repository.OrderRepository;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentRequest;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentResponse;
import sparta.paymentsystemserver.domain.payment.dto.PaymentResultResponse;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.payment.service.PaymentService;
import sparta.paymentsystemserver.domain.payment.service.WebhookSignatureService;
import sparta.paymentsystemserver.domain.refund.service.RefundService;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.client.PortOnePaymentClient;
import sparta.paymentsystemserver.global.redis.RedisBlackListUtil;
import sparta.paymentsystemserver.global.redis.RedisRefreshTokenUtil;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentIntegrationTest {

    @Autowired private PaymentService paymentService;
    @Autowired private RefundService refundService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;

    @MockitoBean private PortOnePaymentClient portOnePaymentClient;
    @MockitoBean private RedisRefreshTokenUtil redisRefreshTokenUtil;
    @MockitoBean private RedisBlackListUtil redisBlackListUtil;
    @MockitoBean private WebhookSignatureService webhookSignatureService;

    @Test
    @DisplayName("통합 - 포인트 전액 결제 생성 → 확정 → 환불 전체 플로우가 정상 동작한다.")
    void fullPaymentFlow_internal() {
        // given - 유저 생성 (포인트 10000)
        User user = new User("통합테스트", "integration@test.com",
                "password", "010-9999-8888", "USR-INT-001");
        user.addPoint(10_000L);
        userRepository.save(user);

        // given - 주문 생성
        Order order = orderRepository.save(
                new Order("ORD-INT-001", "ORD-NUM-INT-001", user,
                        10_000L, OrderStatus.PENDING, LocalDateTime.now())
        );

        // step 1. 결제 시도 생성
        CreatePaymentRequest createRequest =
                new CreatePaymentRequest("ORD-INT-001", 10_000L, 10_000L);
        CreatePaymentResponse createResponse =
                paymentService.createPayment(createRequest, user.getId());

        assertThat(createResponse.success()).isTrue();
        assertThat(createResponse.externalAmount()).isEqualTo(0L); // 포인트 전액 → INTERNAL

        // step 2. 결제 확정
        PaymentResultResponse confirmResponse =
                paymentService.confirmPayment(createResponse.paymentId(), user.getId());

        assertThat(confirmResponse.success()).isTrue();

        // step 3. 결제 후 상태 검증
        var payment = paymentRepository.findByPaymentId(createResponse.paymentId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);

        // step 4. 환불
        PaymentResultResponse refundResponse =
                refundService.cancelPayment(createResponse.paymentId(), user.getId(), "단순 변심");

        assertThat(refundResponse.success()).isTrue();

        // step 5. 환불 후 상태 검증
        var refundedPayment =
                paymentRepository.findByPaymentId(createResponse.paymentId()).orElseThrow();
        assertThat(refundedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refundedPayment.getOrder().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("통합 - 구매확정된 주문은 환불이 불가능하다.")
    void refund_fail_after_purchaseConfirmed() {
        // given
        User user = new User("구매확정테스트", "confirmed@test.com",
                "password", "010-7777-6666", "USR-CONF-001");
        user.addPoint(10_000L);
        userRepository.save(user);

        Order order = orderRepository.save(
                new Order("ORD-CONF-001", "ORD-NUM-CONF-001", user,
                        10_000L, OrderStatus.PENDING, LocalDateTime.now())
        );

        // step 1. 결제 생성 및 확정
        CreatePaymentRequest createRequest =
                new CreatePaymentRequest("ORD-CONF-001", 10_000L, 10_000L);
        CreatePaymentResponse createResponse =
                paymentService.createPayment(createRequest, user.getId());
        paymentService.confirmPayment(createResponse.paymentId(), user.getId());

        // step 2. 구매확정
        order.purchaseConfirmed();

        // step 3. 환불 시도 → 실패 응답
        PaymentResultResponse refundResponse =
                refundService.cancelPayment(createResponse.paymentId(), user.getId(), "단순 변심");

        assertThat(refundResponse.success()).isFalse();
    }
}