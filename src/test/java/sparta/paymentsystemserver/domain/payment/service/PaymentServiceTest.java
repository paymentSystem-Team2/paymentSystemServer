package sparta.paymentsystemserver.domain.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderStatus;
import sparta.paymentsystemserver.domain.order.repository.OrderRepository;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentRequest;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentResponse;
import sparta.paymentsystemserver.domain.payment.dto.PaymentResultResponse;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentProvider;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.client.PortOnePaymentClient;
import sparta.paymentsystemserver.global.client.dto.PortOnePaymentInfo;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PublicIdGenerator publicIdGenerator;

    @Mock
    private PortOnePaymentClient portOnePaymentClient;

    @Mock
    private PaymentTransactionProcessor paymentTransactionProcessor;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentFinalizeService paymentFinalizeService;

    @InjectMocks
    private PaymentService paymentService;
    
    @Test
    @DisplayName("결제 생성 실패 - 존재하지 않는 유저는 예외가 발생한다.")
    void createPayment_userNotFound_fail() {
        // given
        Long userId = -1L;
        CreatePaymentRequest request = new CreatePaymentRequest("ORD-001", 10_000L, 0L);

        given(userRepository.findById(userId)).willReturn(Optional.empty());
                
        // when & then
        assertThatThrownBy(() -> paymentService.createPayment(request, userId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("유저가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("결제 생성 실패 - 다른 사람의 주문은 예외가 발생한다.")
    void createPayment_notMyOrder_fail() {
        // given
        Long userId = 1L;
        CreatePaymentRequest request = new CreatePaymentRequest("ORD-001", 10_000L, 0L);

        User user1 = mock(User.class);
        User user2 = mock(User.class);
        Order order = mock(Order.class);

        given(order.getUser()).willReturn(user2);
        given(user2.getId()).willReturn(2L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user1));
        given(orderRepository.findByOrderId("ORD-001")).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> paymentService.createPayment(request, userId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("본인 주문만 접근할 수 있습니다.");
    }

    @Test
    @DisplayName("결제 생성 실패 - 결제 대기(PENDING)가 아닌 주문은 예외가 발생한다.")
    void createPayment_notPendingStatus_fail() {
        // given
        Long userId = 1L;
        CreatePaymentRequest request = new CreatePaymentRequest("ORD-001", 10_000L, 0L);

        User user = mock(User.class);
        Order order = mock(Order.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderRepository.findByOrderId("ORD-001")).willReturn(Optional.of(order));

        given(user.getId()).willReturn(1L);
        given(order.getUser()).willReturn(user);
        given(order.getStatus()).willReturn(OrderStatus.PAID);

        // when & then
        assertThatThrownBy(() -> paymentService.createPayment(request, userId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("결제 가능한 주문 상태가 아닙니다.");
    }

    @Test
    @DisplayName("결제 생성 실패 - 주문 금액 불일치 시 예외가 발생한다.")
    void createPayment_amountMismatch_fail() {
        // given
        Long userId = 1L;
        CreatePaymentRequest request = new CreatePaymentRequest("ORD-001", 10_000L, 0L);

        User user = mock(User.class);
        Order order = mock(Order.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderRepository.findByOrderId("ORD-001")).willReturn(Optional.of(order));

        given(user.getId()).willReturn(1L);
        given(order.getUser()).willReturn(user);
        given(order.getStatus()).willReturn(OrderStatus.PENDING);
        given(order.getTotalAmount()).willReturn(500_000L);

        // when & then
        assertThatThrownBy(() -> paymentService.createPayment(request, userId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("주문 금액이 일치하지 않습니다.");
    }

    @Test
    @DisplayName("결제 생성 실패 - 음수 포인트 사용 시 예외가 발생한다.")
    void createPayment_negativePoints_fail() {
        // given
        Long userId = 1L;
        CreatePaymentRequest request = new CreatePaymentRequest("ORD-001", 10_000L, -1L);

        User user = mock(User.class);
        Order order = mock(Order.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderRepository.findByOrderId("ORD-001")).willReturn(Optional.of(order));

        given(user.getId()).willReturn(1L);
        given(order.getUser()).willReturn(user);
        given(order.getStatus()).willReturn(OrderStatus.PENDING);
        given(order.getTotalAmount()).willReturn(10_000L);

        // when & then
        assertThatThrownBy(() -> paymentService.createPayment(request, userId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("사용 포인트 값이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("결제 생성 실패 - 보유 포인트보다 많이 사용하려 하면 예외가 발생한다.")
    void createPayment_insufficientPoints_fail() {
        // given
        Long userId = 1L;
        CreatePaymentRequest request = new CreatePaymentRequest("ORD-001", 10_000L, 5_000);

        User user = mock(User.class);
        Order order = mock(Order.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderRepository.findByOrderId("ORD-001")).willReturn(Optional.of(order));

        given(user.getId()).willReturn(1L);
        given(order.getUser()).willReturn(user);
        given(order.getStatus()).willReturn(OrderStatus.PENDING);
        given(order.getTotalAmount()).willReturn(10_000L);
        given(user.getPointBalance()).willReturn(777L);

        // when & then
        assertThatThrownBy(() -> paymentService.createPayment(request, userId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("보유 포인트가 부족합니다.");
    }

    @Test
    @DisplayName("결제 생성 성공 - 포인트 미사용 시 PORTONE provider로 생성된다.")
    void createPayment_noPoints_success() {
        // given
        Long userId = 1L;
        CreatePaymentRequest request = new CreatePaymentRequest("ORD-001", 10_000L, 0);

        User user = mock(User.class);
        Order order = mock(Order.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderRepository.findByOrderId("ORD-001")).willReturn(Optional.of(order));

        given(order.getUser()).willReturn(user);
        given(user.getId()).willReturn(1L);
        given(order.getStatus()).willReturn(OrderStatus.PENDING);
        given(order.getTotalAmount()).willReturn(10_000L);
        given(order.getOrderId()).willReturn("ORD-001");
        given(user.getPointBalance()).willReturn(0L);
        given(publicIdGenerator.generate("PAY")).willReturn("PAY-001");

        // when
        CreatePaymentResponse response = paymentService.createPayment(request, userId);

        // then
        assertThat(response.success()).isTrue();
        assertThat(response.externalAmount()).isEqualTo(10_000L);
    }

    @Test
    @DisplayName("결제 생성 성공 - 포인트 전액 사용 시 INTERNAL provider로 생성된다.")
    void createPayment_fullPoints_success() {
        // given
        Long userId = 1L;
        CreatePaymentRequest request = new CreatePaymentRequest("ORD-001", 10_000L, 10_000L);

        User user = mock(User.class);
        Order order = mock(Order.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(orderRepository.findByOrderId("ORD-001")).willReturn(Optional.of(order));

        given(order.getUser()).willReturn(user);
        given(user.getId()).willReturn(1L);
        given(order.getStatus()).willReturn(OrderStatus.PENDING);
        given(order.getTotalAmount()).willReturn(10_000L);
        given(order.getOrderId()).willReturn("ORD-001");
        given(user.getPointBalance()).willReturn(10_000L);
        given(publicIdGenerator.generate("PAY")).willReturn("PAY-001");

        // when
        CreatePaymentResponse response = paymentService.createPayment(request, userId);

        // then
        assertThat(response.success()).isTrue();
        assertThat(response.externalAmount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("결제 확정 실패 - 다른 사람의 결제는 예외가 발생한다.")
    void confirmPayment_notMyPayment_fail() {
        // given
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        User user = mock(User.class);

        given(payment.getUser()).willReturn(user);
        given(user.getId()).willReturn(2L);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment(paymentId, userId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("본인 주문만 접근할 수 있습니다.");
    }

    @Test
    @DisplayName("결제 확정 성공 - 이미 PAID 상태이면 멱등하게 성공 응답을 반환한다.")
    void confirmPayment_alreadyPaid_success() {
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);
        User user = mock(User.class);

        given(payment.getUser()).willReturn(user);
        given(user.getId()).willReturn(1L);
        given(payment.getStatus()).willReturn(PaymentStatus.PAID);
        given(payment.getPaymentId()).willReturn(paymentId);
        given(payment.getOrder()).willReturn(order);
        given(order.getOrderId()).willReturn("ORD-001");
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        PaymentResultResponse response = paymentService.confirmPayment(paymentId, userId);

        assertThat(response.success()).isTrue();
        then(paymentTransactionProcessor).should(never()).processSuccess(any(), any());
    }

    @Test
    @DisplayName("결제 확정 실패 - READY가 아닌 FAILED 상태는 확정이 불가능하다.")
    void confirmPayment_failedStatus_fail() {
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        User user = mock(User.class);

        given(payment.getUser()).willReturn(user);
        given(user.getId()).willReturn(1L);
        given(payment.getStatus()).willReturn(PaymentStatus.FAILED);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment(paymentId, userId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("확정할 수 없는 결제 상태입니다.");
    }

    @Test
    @DisplayName("결제 확정 성공 - INTERNAL 결제는 PortOne 조회 없이 PaymentFinalizeService에 위임한다.")
    void confirmPayment_internal_success() {
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        Order order = mock(Order.class);
        User user = mock(User.class);

        given(payment.getUser()).willReturn(user);
        given(user.getId()).willReturn(1L);
        given(payment.getStatus()).willReturn(PaymentStatus.READY);
        given(payment.getProvider()).willReturn(PaymentProvider.INTERNAL);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        PaymentResultResponse fakeResult = new PaymentResultResponse(true, paymentId, "ORD-001", "PAID");
        given(paymentFinalizeService.finalizeInternalPayment(paymentId)).willReturn(fakeResult);

        PaymentResultResponse response = paymentService.confirmPayment(paymentId, userId);

        assertThat(response.success()).isTrue();
        then(portOnePaymentClient).should(never()).getPayment(any());
        then(paymentFinalizeService).should().finalizeInternalPayment(paymentId);
    }

    @Test
    @DisplayName("결제 확정 실패 - PortOne 결제 상태가 PAID가 아니면 예외가 발생한다.")
    void confirmPayment_portOneNotPaid_fail() {
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        User user = mock(User.class);
        PortOnePaymentInfo paymentInfo = mock(PortOnePaymentInfo.class);

        given(payment.getUser()).willReturn(user);
        given(user.getId()).willReturn(1L);
        given(payment.getStatus()).willReturn(PaymentStatus.READY);
        given(payment.getProvider()).willReturn(PaymentProvider.PORTONE);
        given(paymentInfo.isPaid()).willReturn(false);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));
        given(portOnePaymentClient.getPayment(paymentId)).willReturn(paymentInfo);

        assertThatThrownBy(() -> paymentService.confirmPayment(paymentId, userId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("PortOne 결제 검증에 실패했습니다.");
    }

    @Test
    @DisplayName("결제 확정 실패 - PortOne 승인 금액과 서버 금액이 다르면 예외가 발생한다.")
    void confirmPayment_amountMismatch_fail() {
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        User user = mock(User.class);
        PortOnePaymentInfo paymentInfo = mock(PortOnePaymentInfo.class);

        given(payment.getUser()).willReturn(user);
        given(user.getId()).willReturn(1L);
        given(payment.getStatus()).willReturn(PaymentStatus.READY);
        given(payment.getProvider()).willReturn(PaymentProvider.PORTONE);
        given(payment.getExternalAmount()).willReturn(10_000L);
        given(paymentInfo.isPaid()).willReturn(true);
        given(paymentInfo.amount()).willReturn(9_000L);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));
        given(portOnePaymentClient.getPayment(paymentId)).willReturn(paymentInfo);

        assertThatThrownBy(() -> paymentService.confirmPayment(paymentId, userId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("결제 승인 금액이 서버 계산 금액과 일치하지 않습니다.");
    }
}