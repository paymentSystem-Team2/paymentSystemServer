package sparta.paymentsystemserver.domain.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.payment.dto.PaymentResultResponse;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentProvider;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.refund.service.RefundFinalizeService;
import sparta.paymentsystemserver.domain.refund.service.RefundService;
import sparta.paymentsystemserver.domain.refund.service.RefundTransactionProcessor;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.global.client.PortOnePaymentClient;
import sparta.paymentsystemserver.global.client.dto.PortOneCancelInfo;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private sparta.paymentsystemserver.domain.refund.repository.RefundRepository refundRepository;

    @Mock
    private PortOnePaymentClient portOnePaymentClient;

    @Mock
    private RefundFinalizeService refundFinalizeService;

    @InjectMocks
    private RefundService refundService;

    @Test
    @DisplayName("환불 실패 - 다른 사람의 결제는 예외가 발생한다.")
    void cancelPayment_notMyPayment_fail() {
        // given
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        User user = mock(User.class);

        given(user.getId()).willReturn(2L);
        given(payment.getUser()).willReturn(user);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> refundService.cancelPayment(paymentId, userId, "단순 변심"))
                .isInstanceOf(PaymentException.class)
                .hasMessage("본인 주문만 접근할 수 있습니다.");
    }

    @Test
    @DisplayName("환불 실패 - PAID가 아닌 결제는 환불이 불가능하다.")
    void cancelPayment_notPaidStatus_fail() {
        // given
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        User user = mock(User.class);

        given(user.getId()).willReturn(1L);
        given(payment.getUser()).willReturn(user);
        given(payment.getStatus()).willReturn(PaymentStatus.READY);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> refundService.cancelPayment(paymentId, userId, "단순 변심"))
                .isInstanceOf(PaymentException.class)
                .hasMessage("환불할 수 없는 결제 상태입니다.");
    }

    @Test
    @DisplayName("환불 실패 - 이미 환불 이력이 있으면 예외가 발생한다.")
    void cancelPayment_alreadyRefundExists_fail() {
        // given
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        User user = mock(User.class);

        given(user.getId()).willReturn(1L);
        given(payment.getId()).willReturn(1L);
        given(payment.getUser()).willReturn(user);
        given(payment.getStatus()).willReturn(PaymentStatus.PAID);
        given(refundRepository.existsByPaymentId(1L)).willReturn(true);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> refundService.cancelPayment(paymentId, userId, "단순 변심"))
                .isInstanceOf(PaymentException.class)
                .hasMessage("이미 환불 완료된 결제입니다.");
    }

    @Test
    @DisplayName("환불 실패 - 구매확정된 주문은 환불이 불가능하다.")
    void cancelPayment_purchaseConfirmed_fail() {
        // given
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        User user = mock(User.class);
        Order order = mock(Order.class);

        given(user.getId()).willReturn(1L);
        given(payment.getId()).willReturn(1L);
        given(payment.getUser()).willReturn(user);
        given(payment.getStatus()).willReturn(PaymentStatus.PAID);
        given(payment.getOrder()).willReturn(order);
        given(order.isConfirmed()).willReturn(true);
        given(refundRepository.existsByPaymentId(1L)).willReturn(false);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        // when
        PaymentResultResponse response = refundService.cancelPayment(paymentId, userId, "단순 변심");

        // then
        assertThat(response.success()).isFalse();
    }

    @Test
    @DisplayName("환불 성공 - 이미 REFUNDED 상태이면 멱등하게 성공 응답을 반환한다.")
    void cancelPayment_alreadyRefund_idempotent_success() {
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        User user = mock(User.class);
        Order order = mock(Order.class);

        given(user.getId()).willReturn(1L);
        given(payment.getUser()).willReturn(user);
        given(payment.getStatus()).willReturn(PaymentStatus.REFUNDED);
        given(payment.getPaymentId()).willReturn(paymentId);
        given(payment.getOrder()).willReturn(order);
        given(order.getOrderId()).willReturn("ORD-001");
        given(order.isConfirmed()).willReturn(false);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        PaymentResultResponse response = refundService.cancelPayment(paymentId, userId, "단순 변심");

        assertThat(response.success()).isTrue();
        then(portOnePaymentClient).should(never()).cancelPayment(any(), any());
    }

    @Test
    @DisplayName("환불 성공 - INTERNAL 결제는 PortOne 취소 없이 RefundFinalizeService에 위임한다.")
    void cancelPayment_internal_success() {
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        User user = mock(User.class);
        Order order = mock(Order.class);

        given(user.getId()).willReturn(1L);
        given(payment.getId()).willReturn(1L);
        given(payment.getUser()).willReturn(user);
        given(payment.getStatus()).willReturn(PaymentStatus.PAID);
        given(payment.getProvider()).willReturn(PaymentProvider.INTERNAL);
        given(payment.getPaymentId()).willReturn(paymentId);
        given(payment.getTotalAmount()).willReturn(10_000L);
        given(payment.getOrder()).willReturn(order);
        given(order.isConfirmed()).willReturn(false);
        given(refundRepository.existsByPaymentId(1L)).willReturn(false);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        PaymentResultResponse fakeResult = new PaymentResultResponse(true, paymentId, "ORD-001", "REFUNDED");
        given(refundFinalizeService.finalizeRefund(eq(paymentId), eq(10_000L), eq("단순 변심"), eq("INTERNAL")))
                .willReturn(fakeResult);

        PaymentResultResponse response = refundService.cancelPayment(paymentId, userId, "단순 변심");

        assertThat(response.success()).isTrue();
        then(portOnePaymentClient).should(never()).cancelPayment(any(), any());
        then(refundFinalizeService).should().finalizeRefund(paymentId, 10_000L, "단순 변심", "INTERNAL");
    }

    @Test
    @DisplayName("환불 성공 - PORTONE 결제는 PortOne 취소 API 호출 후 RefundFinalizeService에 위임한다.")
    void cancelPayment_portone_success() {
        String paymentId = "PAY-001";
        Long userId = 1L;

        Payment payment = mock(Payment.class);
        User user = mock(User.class);
        Order order = mock(Order.class);
        PortOneCancelInfo cancelInfo = mock(PortOneCancelInfo.class);

        given(user.getId()).willReturn(1L);
        given(payment.getId()).willReturn(1L);
        given(payment.getUser()).willReturn(user);
        given(payment.getStatus()).willReturn(PaymentStatus.PAID);
        given(payment.getProvider()).willReturn(PaymentProvider.PORTONE);
        given(payment.getPaymentId()).willReturn(paymentId);
        given(payment.getOrder()).willReturn(order);
        given(order.isConfirmed()).willReturn(false);
        given(cancelInfo.isCancelled()).willReturn(true);
        given(cancelInfo.cancellationId()).willReturn("CANCEL-001");
        given(cancelInfo.cancelledAmount()).willReturn(10_000L);
        given(refundRepository.existsByPaymentId(1L)).willReturn(false);
        given(portOnePaymentClient.cancelPayment(paymentId, "단순 변심")).willReturn(cancelInfo);
        given(paymentRepository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        PaymentResultResponse fakeResult = new PaymentResultResponse(true, paymentId, "ORD-001", "REFUNDED");
        given(refundFinalizeService.finalizeRefund(eq(paymentId), eq(10_000L), eq("단순 변심"), eq("CANCEL-001")))
                .willReturn(fakeResult);

        PaymentResultResponse response = refundService.cancelPayment(paymentId, userId, "단순 변심");

        assertThat(response.success()).isTrue();
        then(portOnePaymentClient).should().cancelPayment(paymentId, "단순 변심");
        then(refundFinalizeService).should().finalizeRefund(paymentId, 10_000L, "단순 변심", "CANCEL-001");
    }
}